package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.request.ParkingLocationRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.ParkingLocationResponseDto;
import com.projectwork.Smart.Parking.System.entity.BookingStatus;
import com.projectwork.Smart.Parking.System.entity.ParkingLocation;
import com.projectwork.Smart.Parking.System.entity.ParkingSlot;
import com.projectwork.Smart.Parking.System.entity.ParkingSlotStatus;
import com.projectwork.Smart.Parking.System.entity.User;
import com.projectwork.Smart.Parking.System.entity.UserRole;
import com.projectwork.Smart.Parking.System.entity.VehicleType;
import com.projectwork.Smart.Parking.System.repository.BookingRepository;
import com.projectwork.Smart.Parking.System.repository.ParkingLocationRepository;
import com.projectwork.Smart.Parking.System.repository.ParkingSlotRepository;
import com.projectwork.Smart.Parking.System.repository.PaymentRepository;
import com.projectwork.Smart.Parking.System.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParkingServiceImplTest {

    @Mock
    private DijkstraService dijkstraService;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ParkingLocationRepository parkingLocationRepository;

    @Mock
    private ParkingSlotRepository parkingSlotRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private UserRepository userRepository;

    private ParkingServiceImpl parkingService;

    @BeforeEach
    void setUp() {
        parkingService = new ParkingServiceImpl(
                dijkstraService,
                bookingRepository,
                parkingLocationRepository,
                parkingSlotRepository,
                paymentRepository,
                userRepository);
    }

    @Test
    void addParkingLocation_shouldGenerateExpectedTwoAndFourWheelerSlots() {
        User vendor = buildUser("vendor@example.com", UserRole.VENDOR);
        ParkingLocationRequestDto request = buildParkingRequest(2, 3);

        when(userRepository.findByEmailAndDeletedAtIsNull("vendor@example.com")).thenReturn(Optional.of(vendor));
        when(parkingLocationRepository.existsByNameIgnoreCaseAndVendorAndDeletedAtIsNull("Thamel Plaza", vendor))
                .thenReturn(false);
        when(parkingLocationRepository.save(any(ParkingLocation.class))).thenAnswer(invocation -> {
            ParkingLocation location = invocation.getArgument(0);
            ReflectionTestUtils.setField(location, "id", UUID.randomUUID());
            return location;
        });

        ParkingLocationResponseDto response =
                parkingService.addParkingLocation(request, "vendor@example.com");

        assertEquals(5, response.getTotalSlots());
        assertEquals(5, response.getAvailableSlots());
        assertEquals(2, response.getTotalFourWheelerSlots());
        assertEquals(3, response.getTotalTwoWheelerSlots());

        ArgumentCaptor<List<ParkingSlot>> slotsCaptor = ArgumentCaptor.forClass(List.class);
        verify(parkingSlotRepository, times(2)).saveAll(slotsCaptor.capture());

        List<ParkingSlot> allSlots = slotsCaptor.getAllValues()
                .stream()
                .flatMap(List::stream)
                .toList();

        assertEquals(List.of("FW-1", "FW-2", "TW-1", "TW-2", "TW-3"),
                allSlots.stream().map(ParkingSlot::getSlotNumber).toList());
        assertEquals(List.of(
                        VehicleType.FOUR_WHEELER,
                        VehicleType.FOUR_WHEELER,
                        VehicleType.TWO_WHEELER,
                        VehicleType.TWO_WHEELER,
                        VehicleType.TWO_WHEELER),
                allSlots.stream().map(ParkingSlot::getVehicleType).toList());
        assertTrue(allSlots.stream().allMatch(slot -> slot.getStatus() == ParkingSlotStatus.AVAILABLE));
        assertTrue(allSlots.stream().allMatch(slot -> slot.getLocation().getId() != null));
        verify(parkingLocationRepository).save(any(ParkingLocation.class));
    }

    @Test
    void addParkingLocation_shouldRejectZeroTotalCapacity() {
        User vendor = buildUser("vendor@example.com", UserRole.VENDOR);
        ParkingLocationRequestDto request = buildParkingRequest(0, 0);

        when(userRepository.findByEmailAndDeletedAtIsNull("vendor@example.com")).thenReturn(Optional.of(vendor));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> parkingService.addParkingLocation(request, "vendor@example.com"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(parkingLocationRepository, never()).save(any(ParkingLocation.class));
        verify(parkingSlotRepository, never()).saveAll(any());
    }

    @Test
    void deleteParkingLocation_shouldRejectLocationWithActiveBookings() {
        User vendor = buildUser("vendor@example.com", UserRole.VENDOR);
        ParkingLocation parking = buildParkingLocation(vendor);
        UUID parkingId = parking.getId();

        when(parkingLocationRepository.findByIdAndDeletedAtIsNull(parkingId))
                .thenReturn(Optional.of(parking));
        when(bookingRepository.existsByParkingLocationAndStatusInAndDeletedAtIsNull(
                parking,
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)
        )).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> parkingService.deleteParkingLocation(parkingId, "vendor@example.com"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Cannot delete parking location with active bookings.", exception.getReason());
        verify(parkingLocationRepository, never()).save(any(ParkingLocation.class));
    }

    @Test
    void deleteParkingLocation_shouldSoftDeleteLocationWhenNoActiveBookingsExist() {
        User vendor = buildUser("vendor@example.com", UserRole.VENDOR);
        ParkingLocation parking = buildParkingLocation(vendor);
        UUID parkingId = parking.getId();

        when(parkingLocationRepository.findByIdAndDeletedAtIsNull(parkingId))
                .thenReturn(Optional.of(parking));
        when(bookingRepository.existsByParkingLocationAndStatusInAndDeletedAtIsNull(
                parking,
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)
        )).thenReturn(false);

        parkingService.deleteParkingLocation(parkingId, "vendor@example.com");

        assertTrue(parking.isDeleted());
        verify(parkingLocationRepository).save(parking);
    }

    private ParkingLocationRequestDto buildParkingRequest(int fourWheelerSlots, int twoWheelerSlots) {
        ParkingLocationRequestDto request = new ParkingLocationRequestDto();
        request.setName(" Thamel Plaza ");
        request.setAddress("Thamel, Kathmandu");
        request.setLatitude(27.71520);
        request.setLongitude(85.31250);
        request.setTotalFourWheelerSlots(fourWheelerSlots);
        request.setTotalTwoWheelerSlots(twoWheelerSlots);
        request.setFourWheelerRatePerHour(100.0);
        request.setTwoWheelerRatePerHour(50.0);
        return request;
    }

    private User buildUser(String email, UserRole role) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.setName("Test User");
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setPhone("9800000000");
        user.setRole(role);
        user.setApproved(true);
        return user;
    }

    private ParkingLocation buildParkingLocation(User vendor) {
        ParkingLocation parking = new ParkingLocation();
        ReflectionTestUtils.setField(parking, "id", UUID.randomUUID());
        parking.setName("Thamel Plaza");
        parking.setAddress("Thamel, Kathmandu");
        parking.setLatitude(27.71520);
        parking.setLongitude(85.31250);
        parking.setTotalFourWheelerSlots(2);
        parking.setAvailableFourWheelerSlots(2);
        parking.setTotalTwoWheelerSlots(3);
        parking.setAvailableTwoWheelerSlots(3);
        parking.setFourWheelerRatePerHour(100.0);
        parking.setTwoWheelerRatePerHour(50.0);
        parking.setVendor(vendor);
        return parking;
    }
}
