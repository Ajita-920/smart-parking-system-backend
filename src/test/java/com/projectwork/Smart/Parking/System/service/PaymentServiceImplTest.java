package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.response.PaymentResponseDto;
import com.projectwork.Smart.Parking.System.entity.Booking;
import com.projectwork.Smart.Parking.System.entity.BookingStatus;
import com.projectwork.Smart.Parking.System.entity.ParkingLocation;
import com.projectwork.Smart.Parking.System.entity.ParkingSlot;
import com.projectwork.Smart.Parking.System.entity.ParkingSlotStatus;
import com.projectwork.Smart.Parking.System.entity.Payment;
import com.projectwork.Smart.Parking.System.entity.PaymentMethod;
import com.projectwork.Smart.Parking.System.entity.PaymentStatus;
import com.projectwork.Smart.Parking.System.entity.VehicleType;
import com.projectwork.Smart.Parking.System.repository.BookingRepository;
import com.projectwork.Smart.Parking.System.repository.ParkingLocationRepository;
import com.projectwork.Smart.Parking.System.repository.ParkingSlotRepository;
import com.projectwork.Smart.Parking.System.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ParkingLocationRepository parkingLocationRepository;

    @Mock
    private ParkingSlotRepository parkingSlotRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(
                bookingRepository,
                parkingLocationRepository,
                parkingSlotRepository,
                paymentRepository,
                emailService);
        ReflectionTestUtils.setField(paymentService, "restClient", restClient);
        ReflectionTestUtils.setField(paymentService, "khaltiVerifyUrl", "https://khalti.test/verify");
        ReflectionTestUtils.setField(paymentService, "khaltiSecretKey", "test-secret-key");
    }

    @Test
    void verifyKhaltiPayment_shouldMarkPaymentSuccessAndConfirmBooking() {
        Booking booking = buildPendingBookingWithReservedSlot();
        Payment payment = buildPendingPayment(booking, "test-pidx");

        mockKhaltiVerifyResponse("Completed", 2250);
        when(paymentRepository.findByPidxAndDeletedAtIsNull("test-pidx")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponseDto response = paymentService.verifyKhaltiPayment("test-pidx");

        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals(new BigDecimal("22.50"), payment.getAmount());
        assertNotNull(payment.getPaidAt());
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertEquals(ParkingSlotStatus.BOOKED, booking.getSlot().getStatus());
        assertEquals(new BigDecimal("22.50"), response.getAmount());
        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        assertEquals("test-pidx", response.getPidx());
        verify(parkingSlotRepository).save(booking.getSlot());
        verify(bookingRepository).save(booking);
        verify(paymentRepository).save(payment);
    }

    @Test
    void verifyKhaltiPayment_shouldMarkPaymentFailedWhenKhaltiReturnsFailed() {
        Booking booking = buildPendingBookingWithReservedSlot();
        Payment payment = buildPendingPayment(booking, "failed-pidx");

        mockKhaltiVerifyResponse("Failed", null);
        when(paymentRepository.findByPidxAndDeletedAtIsNull("failed-pidx")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponseDto response = paymentService.verifyKhaltiPayment("failed-pidx");

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals(ParkingSlotStatus.AVAILABLE, booking.getSlot().getStatus());
        assertEquals(2, booking.getParkingLocation().getAvailableFourWheelerSlots());
        assertEquals(PaymentStatus.FAILED, response.getStatus());
        verify(parkingSlotRepository).save(booking.getSlot());
        verify(parkingLocationRepository).save(booking.getParkingLocation());
        verify(bookingRepository).save(booking);
        verify(paymentRepository).save(payment);
    }

    private void mockKhaltiVerifyResponse(String status, Integer totalAmountInPaisa) {
        Map<String, Object> khaltiResponse = new java.util.HashMap<>();
        khaltiResponse.put("status", status);
        if (totalAmountInPaisa != null) {
            khaltiResponse.put("total_amount", totalAmountInPaisa);
        }

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("https://khalti.test/verify")).thenReturn(requestBodySpec);
        when(requestBodySpec.header("Authorization", "test-secret-key")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Map.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(eq(Map.class))).thenReturn(khaltiResponse);
    }

    private Payment buildPendingPayment(Booking booking, String pidx) {
        Payment payment = new Payment();
        ReflectionTestUtils.setField(payment, "id", UUID.randomUUID());
        payment.setBooking(booking);
        payment.setAmount(new BigDecimal("200.00"));
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentMethod(PaymentMethod.KHALTI);
        payment.setTransactionId("TXN-TEST");
        payment.setPidx(pidx);
        return payment;
    }

    private Booking buildPendingBookingWithReservedSlot() {
        ParkingLocation location = new ParkingLocation();
        ReflectionTestUtils.setField(location, "id", UUID.randomUUID());
        location.setName("Thamel Plaza");
        location.setAddress("Thamel, Kathmandu");
        location.setLatitude(27.71520);
        location.setLongitude(85.31250);
        location.setTotalFourWheelerSlots(2);
        location.setAvailableFourWheelerSlots(1);
        location.setTotalTwoWheelerSlots(0);
        location.setAvailableTwoWheelerSlots(0);

        ParkingSlot slot = new ParkingSlot();
        ReflectionTestUtils.setField(slot, "id", UUID.randomUUID());
        slot.setLocation(location);
        slot.setSlotNumber("FW-1");
        slot.setVehicleType(VehicleType.FOUR_WHEELER);
        slot.setStatus(ParkingSlotStatus.RESERVED);

        Booking booking = new Booking();
        ReflectionTestUtils.setField(booking, "id", UUID.randomUUID());
        booking.setParkingLocation(location);
        booking.setSlot(slot);
        booking.setVehicleType(VehicleType.FOUR_WHEELER);
        booking.setVehicleNumber("BA-01-PA-1234");
        booking.setStartTime(LocalDateTime.of(2026, 7, 1, 10, 0));
        booking.setEndTime(LocalDateTime.of(2026, 7, 1, 11, 30));
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalAmount(new BigDecimal("200.00"));
        return booking;
    }
}
