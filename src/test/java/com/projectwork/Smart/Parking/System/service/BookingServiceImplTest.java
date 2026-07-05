package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.request.BookingRequestDto;
import com.projectwork.Smart.Parking.System.dto.request.VendorBookingStatusRequestDto;
import com.projectwork.Smart.Parking.System.dto.response.BookingCancelResponseDto;
import com.projectwork.Smart.Parking.System.dto.response.BookingResponseDto;
import com.projectwork.Smart.Parking.System.entity.Booking;
import com.projectwork.Smart.Parking.System.entity.BookingStatus;
import com.projectwork.Smart.Parking.System.entity.ParkingLocation;
import com.projectwork.Smart.Parking.System.entity.ParkingSlot;
import com.projectwork.Smart.Parking.System.entity.ParkingSlotStatus;
import com.projectwork.Smart.Parking.System.entity.PaymentStatus;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    private static final LocalDateTime START_TIME = LocalDateTime.of(2026, 7, 1, 10, 0);

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ParkingLocationRepository parkingLocationRepository;

    @Mock
    private ParkingSlotRepository parkingSlotRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private EmailService emailService;

    private BookingServiceImpl bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingServiceImpl(
                bookingRepository,
                parkingLocationRepository,
                parkingSlotRepository,
                userRepository,
                paymentRepository,
                emailService);
    }

    @Test
    void createBooking_shouldRejectWhenSlotVehicleTypeDoesNotMatchRequest() {
        User driver = buildUser("driver@example.com", UserRole.DRIVER);
        ParkingLocation location = buildLocation(1, 1);
        ParkingSlot slot = buildSlot(location, VehicleType.TWO_WHEELER, ParkingSlotStatus.AVAILABLE);
        BookingRequestDto request = buildBookingRequest(location.getId(), slot.getId(),
                VehicleType.FOUR_WHEELER, START_TIME, START_TIME.plusMinutes(90));

        when(userRepository.findByEmailAndDeletedAtIsNull("driver@example.com")).thenReturn(Optional.of(driver));
        when(parkingLocationRepository.findByIdAndDeletedAtIsNull(location.getId())).thenReturn(Optional.of(location));
        when(parkingSlotRepository.findByIdAndDeletedAtIsNullForUpdate(slot.getId())).thenReturn(Optional.of(slot));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> bookingService.createBooking(request, "driver@example.com"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(ParkingSlotStatus.AVAILABLE, slot.getStatus());
        assertEquals(1, location.getAvailableFourWheelerSlots());
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(parkingSlotRepository, never()).save(any(ParkingSlot.class));
    }

    @Test
    void createBooking_shouldCalculateNinetyMinutesAsTwoBillableHours() {
        User driver = buildUser("driver@example.com", UserRole.DRIVER);
        ParkingLocation location = buildLocation(2, 0);
        location.setFourWheelerRatePerHour(100.0);
        ParkingSlot slot = buildSlot(location, VehicleType.FOUR_WHEELER, ParkingSlotStatus.AVAILABLE);
        BookingRequestDto request = buildBookingRequest(location.getId(), slot.getId(),
                VehicleType.FOUR_WHEELER, START_TIME, START_TIME.plusMinutes(90));

        when(userRepository.findByEmailAndDeletedAtIsNull("driver@example.com")).thenReturn(Optional.of(driver));
        when(parkingLocationRepository.findByIdAndDeletedAtIsNull(location.getId())).thenReturn(Optional.of(location));
        when(parkingSlotRepository.findByIdAndDeletedAtIsNullForUpdate(slot.getId())).thenReturn(Optional.of(slot));
        when(parkingSlotRepository.save(any(ParkingSlot.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(parkingLocationRepository.save(any(ParkingLocation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            ReflectionTestUtils.setField(booking, "id", UUID.randomUUID());
            return booking;
        });
        when(paymentRepository.findFirstByBooking_IdAndStatusAndDeletedAtIsNullOrderByPaidAtDesc(
                any(UUID.class), any(PaymentStatus.class))).thenReturn(Optional.empty());
        when(paymentRepository.findFirstByBooking_IdAndDeletedAtIsNullOrderByCreatedAtDesc(any(UUID.class)))
                .thenReturn(Optional.empty());

        BookingResponseDto response = bookingService.createBooking(request, "driver@example.com");

        assertEquals(new BigDecimal("200.0"), response.getTotalAmount());
        assertEquals(ParkingSlotStatus.RESERVED, slot.getStatus());
        assertEquals(1, location.getAvailableFourWheelerSlots());
    }

    @Test
    void createBooking_shouldCalculateSixtyAndSixtyOneMinuteBoundaries() {
        assertEquals(new BigDecimal("100.0"), createBookingAndReturnAmount(60));
        assertEquals(new BigDecimal("200.0"), createBookingAndReturnAmount(61));
    }

    @Test
    void cancelBooking_shouldCancelPendingBookingAndReleaseReservedSlot() {
        User driver = buildUser("driver@example.com", UserRole.DRIVER);
        ParkingLocation location = buildLocation(2, 0);
        location.setAvailableFourWheelerSlots(1);
        ParkingSlot slot = buildSlot(location, VehicleType.FOUR_WHEELER, ParkingSlotStatus.RESERVED);
        Booking booking = buildBooking(driver, location, slot, BookingStatus.PENDING);

        when(userRepository.findByEmailAndDeletedAtIsNull("driver@example.com")).thenReturn(Optional.of(driver));
        when(bookingRepository.findByIdAndDriverAndDeletedAtIsNull(booking.getId(), driver))
                .thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingCancelResponseDto response = bookingService.cancelBooking(booking.getId(), "driver@example.com");

        assertEquals(BookingStatus.CANCELLED.name(), response.getStatus());
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertNotNull(booking.getCancelledAt());
        assertEquals(ParkingSlotStatus.AVAILABLE, slot.getStatus());
        assertEquals(2, location.getAvailableFourWheelerSlots());
        verify(parkingSlotRepository).save(slot);
        verify(parkingLocationRepository).save(location);
        verify(bookingRepository).save(booking);
    }

    @Test
    void expirePendingBookings_shouldCancelExpiredPendingBookingsAndReleaseReservedSlots() {
        User driver = buildUser("driver@example.com", UserRole.DRIVER);
        ParkingLocation location = buildLocation(2, 0);
        location.setAvailableFourWheelerSlots(1);
        ParkingSlot slot = buildSlot(location, VehicleType.FOUR_WHEELER, ParkingSlotStatus.RESERVED);
        Booking booking = buildBooking(driver, location, slot, BookingStatus.PENDING);
        ReflectionTestUtils.setField(booking, "createdAt", Instant.now().minusSeconds(1800));
        ReflectionTestUtils.setField(bookingService, "pendingBookingExpirationMinutes", 15L);

        when(bookingRepository.findByStatusAndCreatedAtBeforeAndDeletedAtIsNull(
                eq(BookingStatus.PENDING),
                any(Instant.class)
        )).thenReturn(List.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        bookingService.expirePendingBookings();

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertNotNull(booking.getCancelledAt());
        assertEquals(ParkingSlotStatus.AVAILABLE, slot.getStatus());
        assertEquals(2, location.getAvailableFourWheelerSlots());
        verify(parkingSlotRepository).save(slot);
        verify(parkingLocationRepository).save(location);
        verify(bookingRepository).save(booking);
    }

    @Test
    void updateVendorBookingStatus_shouldCompleteUsingConfiguredBusinessTimeZone() {
        User vendor = buildUser("vendor@example.com", UserRole.VENDOR);
        ParkingLocation location = buildLocation(2, 0);
        location.setVendor(vendor);
        location.setAvailableFourWheelerSlots(1);
        ParkingSlot slot = buildSlot(location, VehicleType.FOUR_WHEELER, ParkingSlotStatus.OCCUPIED);
        Booking booking = buildBooking(buildUser("driver@example.com", UserRole.DRIVER),
                location,
                slot,
                BookingStatus.CONFIRMED);
        LocalDateTime kathmanduNow = LocalDateTime.now(ZoneId.of("Asia/Kathmandu"));
        booking.setStartTime(kathmanduNow.minusMinutes(10));
        booking.setEndTime(kathmanduNow.plusMinutes(50));

        VendorBookingStatusRequestDto request = new VendorBookingStatusRequestDto();
        request.setAction("COMPLETE");
        ReflectionTestUtils.setField(bookingService, "appTimeZone", "Asia/Kathmandu");

        when(bookingRepository.findByIdAndDeletedAtIsNull(booking.getId())).thenReturn(Optional.of(booking));
        when(userRepository.findByEmailAndDeletedAtIsNull("vendor@example.com")).thenReturn(Optional.of(vendor));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.findFirstByBooking_IdAndStatusAndDeletedAtIsNullOrderByPaidAtDesc(
                any(UUID.class), any(PaymentStatus.class))).thenReturn(Optional.empty());
        when(paymentRepository.findFirstByBooking_IdAndDeletedAtIsNullOrderByCreatedAtDesc(any(UUID.class)))
                .thenReturn(Optional.empty());

        BookingResponseDto response = bookingService.updateVendorBookingStatus(
                booking.getId(),
                request,
                "vendor@example.com");

        assertEquals(BookingStatus.COMPLETED, response.getStatus());
        assertEquals(ParkingSlotStatus.AVAILABLE, slot.getStatus());
        assertTrue(booking.getEndTime().isAfter(booking.getStartTime()));
        assertEquals(new BigDecimal("100.0"), response.getTotalAmount());
        assertEquals(2, location.getAvailableFourWheelerSlots());
    }

    private BigDecimal createBookingAndReturnAmount(int minutes) {
        User driver = buildUser("driver-" + minutes + "@example.com", UserRole.DRIVER);
        ParkingLocation location = buildLocation(2, 0);
        location.setFourWheelerRatePerHour(100.0);
        ParkingSlot slot = buildSlot(location, VehicleType.FOUR_WHEELER, ParkingSlotStatus.AVAILABLE);
        BookingRequestDto request = buildBookingRequest(location.getId(), slot.getId(),
                VehicleType.FOUR_WHEELER, START_TIME, START_TIME.plusMinutes(minutes));

        when(userRepository.findByEmailAndDeletedAtIsNull(driver.getEmail())).thenReturn(Optional.of(driver));
        when(parkingLocationRepository.findByIdAndDeletedAtIsNull(location.getId())).thenReturn(Optional.of(location));
        when(parkingSlotRepository.findByIdAndDeletedAtIsNullForUpdate(slot.getId())).thenReturn(Optional.of(slot));
        when(parkingSlotRepository.save(any(ParkingSlot.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(parkingLocationRepository.save(any(ParkingLocation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            ReflectionTestUtils.setField(booking, "id", UUID.randomUUID());
            return booking;
        });
        when(paymentRepository.findFirstByBooking_IdAndStatusAndDeletedAtIsNullOrderByPaidAtDesc(
                any(UUID.class), any(PaymentStatus.class))).thenReturn(Optional.empty());
        when(paymentRepository.findFirstByBooking_IdAndDeletedAtIsNullOrderByCreatedAtDesc(any(UUID.class)))
                .thenReturn(Optional.empty());

        return bookingService.createBooking(request, driver.getEmail()).getTotalAmount();
    }

    private BookingRequestDto buildBookingRequest(
            UUID locationId,
            UUID slotId,
            VehicleType vehicleType,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        BookingRequestDto request = new BookingRequestDto();
        request.setParkingLocationId(locationId);
        request.setSlotId(slotId);
        request.setVehicleType(vehicleType.name());
        request.setVehicleNumber("BA-01-PA-1234");
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        return request;
    }

    private Booking buildBooking(
            User driver,
            ParkingLocation location,
            ParkingSlot slot,
            BookingStatus status) {
        Booking booking = new Booking();
        ReflectionTestUtils.setField(booking, "id", UUID.randomUUID());
        booking.setDriver(driver);
        booking.setCustomerName(driver.getName());
        booking.setCustomerPhone(driver.getPhone());
        booking.setParkingLocation(location);
        booking.setSlot(slot);
        booking.setVehicleType(slot.getVehicleType());
        booking.setVehicleNumber("BA-01-PA-1234");
        booking.setStartTime(START_TIME);
        booking.setEndTime(START_TIME.plusMinutes(90));
        booking.setStatus(status);
        booking.setTotalAmount(new BigDecimal("200.00"));
        return booking;
    }

    private ParkingSlot buildSlot(ParkingLocation location, VehicleType vehicleType, ParkingSlotStatus status) {
        ParkingSlot slot = new ParkingSlot();
        ReflectionTestUtils.setField(slot, "id", UUID.randomUUID());
        slot.setLocation(location);
        slot.setVehicleType(vehicleType);
        slot.setStatus(status);
        slot.setSlotNumber(vehicleType == VehicleType.FOUR_WHEELER ? "FW-1" : "TW-1");
        return slot;
    }

    private ParkingLocation buildLocation(int fourWheelerSlots, int twoWheelerSlots) {
        ParkingLocation location = new ParkingLocation();
        ReflectionTestUtils.setField(location, "id", UUID.randomUUID());
        location.setName("Thamel Plaza");
        location.setAddress("Thamel, Kathmandu");
        location.setLatitude(27.71520);
        location.setLongitude(85.31250);
        location.setTotalFourWheelerSlots(fourWheelerSlots);
        location.setAvailableFourWheelerSlots(fourWheelerSlots);
        location.setTotalTwoWheelerSlots(twoWheelerSlots);
        location.setAvailableTwoWheelerSlots(twoWheelerSlots);
        location.setFourWheelerRatePerHour(100.0);
        location.setTwoWheelerRatePerHour(50.0);
        location.setVendor(buildUser("vendor@example.com", UserRole.VENDOR));
        return location;
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
}
