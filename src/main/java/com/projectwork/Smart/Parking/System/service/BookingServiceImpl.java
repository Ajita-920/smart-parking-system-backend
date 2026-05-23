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
import com.projectwork.Smart.Parking.System.entity.Payment;
import com.projectwork.Smart.Parking.System.entity.PaymentStatus;
import com.projectwork.Smart.Parking.System.entity.RefundStatus;
import com.projectwork.Smart.Parking.System.entity.User;
import com.projectwork.Smart.Parking.System.entity.UserRole;
import com.projectwork.Smart.Parking.System.entity.VehicleType;
import com.projectwork.Smart.Parking.System.repository.BookingRepository;
import com.projectwork.Smart.Parking.System.repository.ParkingLocationRepository;
import com.projectwork.Smart.Parking.System.repository.ParkingSlotRepository;
import com.projectwork.Smart.Parking.System.repository.PaymentRepository;
import com.projectwork.Smart.Parking.System.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implements booking lifecycle rules, including slot reservation, cancellation,
 * and vendor status updates.
 */
@Service
public class BookingServiceImpl implements BookingService {

        private static final BigDecimal DEFAULT_HOURLY_RATE = new BigDecimal("100.00");

        private final BookingRepository bookingRepository;
        private final ParkingLocationRepository parkingLocationRepository;
        private final ParkingSlotRepository parkingSlotRepository;
        private final UserRepository userRepository;
        private final PaymentRepository paymentRepository;

        public BookingServiceImpl(
                        BookingRepository bookingRepository,
                        ParkingLocationRepository parkingLocationRepository,
                        ParkingSlotRepository parkingSlotRepository,
                        UserRepository userRepository,
                        PaymentRepository paymentRepository) {
                this.bookingRepository = bookingRepository;
                this.parkingLocationRepository = parkingLocationRepository;
                this.parkingSlotRepository = parkingSlotRepository;
                this.userRepository = userRepository;
                this.paymentRepository = paymentRepository;
        }

        @Override
        @Transactional
        public BookingResponseDto createBooking(BookingRequestDto request, String currentUserEmail) {
                User driver = userRepository.findByEmailAndDeletedAtIsNull(normalizeEmail(currentUserEmail))
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "User not found."));

                if (driver.getRole() != UserRole.DRIVER) {
                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "Only drivers can create bookings.");
                }

                ParkingLocation location = parkingLocationRepository
                                .findByIdAndDeletedAtIsNull(request.getParkingLocationId())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Parking location not found."));

                VehicleType vehicleType = parseVehicleType(request.getVehicleType());

                validateAvailableSlotCount(location, vehicleType);

                ParkingSlot slot = parkingSlotRepository.findByIdAndDeletedAtIsNull(request.getSlotId())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Parking slot not found."));

                if (!slot.getLocation().getId().equals(location.getId())) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Selected slot does not belong to this parking location.");
                }

                if (slot.getVehicleType() != vehicleType) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Selected slot does not match the requested vehicle type.");
                }

                if (slot.getStatus() != ParkingSlotStatus.AVAILABLE) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "Selected slot is not available.");
                }

                slot.markReserved();
                decrementAvailableSlotCount(location, vehicleType);

                Booking booking = new Booking();
                booking.setDriver(driver);
                booking.setParkingLocation(location);
                booking.setSlot(slot);
                booking.setVehicleType(vehicleType);
                booking.setStartTime(request.getStartTime());
                booking.setEndTime(request.getEndTime());
                booking.setStatus(BookingStatus.CONFIRMED);
                booking.setTotalAmount(
                                calculateAmount(location, vehicleType, request.getStartTime(), request.getEndTime()));

                ParkingSlot savedSlot = parkingSlotRepository.save(slot);
                ParkingLocation savedLocation = parkingLocationRepository.save(location);

                booking.setSlot(savedSlot);
                booking.setParkingLocation(savedLocation);

                Booking savedBooking = bookingRepository.save(booking);

                BookingResponseDto response = mapToResponse(savedBooking);
                response.setMessage("Booking created successfully.");
                return response;
        }

        @Override
        @Transactional(readOnly = true)
        public List<BookingResponseDto> getMyBookings(String currentUserEmail) {
                User driver = userRepository.findByEmailAndDeletedAtIsNull(normalizeEmail(currentUserEmail))
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "User not found."));

                return bookingRepository.findByDriverAndDeletedAtIsNull(driver)
                                .stream()
                                .map(this::mapToResponse)
                                .toList();
        }

        @Override
        @Transactional(readOnly = true)
        public BookingResponseDto getBookingById(java.util.UUID id, String currentUserEmail) {
                Booking booking = bookingRepository.findByIdAndDeletedAtIsNull(id)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Booking not found."));

                User currentUser = userRepository.findByEmailAndDeletedAtIsNull(normalizeEmail(currentUserEmail))
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "User not found."));

                boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
                boolean isBookingDriver = booking.getDriver().getId().equals(currentUser.getId());
                boolean isLocationVendor = currentUser.getRole() == UserRole.VENDOR
                                && booking.getParkingLocation().getVendor().getId().equals(currentUser.getId());

                if (!isAdmin && !isBookingDriver && !isLocationVendor) {
                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "You do not have permission to view this booking.");
                }

                BookingResponseDto response = mapToResponse(booking);
                response.setMessage("Booking fetched successfully.");
                return response;
        }

        @Override
        @Transactional
        public BookingCancelResponseDto cancelBooking(java.util.UUID bookingId, String email) {
                User driver = userRepository.findByEmailAndDeletedAtIsNull(normalizeEmail(email))
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "User not found."));

                Booking booking = bookingRepository.findByIdAndDriverAndDeletedAtIsNull(bookingId, driver)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Booking not found for current user."));

                if (booking.getStatus() == BookingStatus.CANCELLED) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "Booking is already cancelled.");
                }

                if (booking.getStatus() == BookingStatus.COMPLETED) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "Completed booking cannot be cancelled.");
                }

                booking.markCancelled();

                releaseSlotAndIncreaseAvailability(booking);

                boolean refundEligible = isRefundEligible(booking);

                BookingCancelResponseDto response = new BookingCancelResponseDto();
                response.setBookingId(booking.getId());
                response.setStatus(BookingStatus.CANCELLED.name());
                response.setRefunded(false);

                if (refundEligible) {
                        BigDecimal refundAmount = booking.getTotalAmount() != null
                                        ? booking.getTotalAmount()
                                        : BigDecimal.ZERO;

                        Optional<Payment> successfulPayment = paymentRepository
                                        .findFirstByBooking_IdAndStatusAndDeletedAtIsNullOrderByPaidAtDesc(
                                                        booking.getId(),
                                                        PaymentStatus.SUCCESS);

                        if (successfulPayment.isPresent()) {
                                Payment payment = successfulPayment.get();
                                payment.markRefundPending(refundAmount);
                                paymentRepository.save(payment);

                                response.setRefundStatus(RefundStatus.PENDING.name());
                                response.setRefundAmount(refundAmount);
                                response.setMessage("Booking cancelled. Refund is pending.");
                        } else {
                                response.setRefundStatus(RefundStatus.NONE.name());
                                response.setRefundAmount(BigDecimal.ZERO);
                                response.setMessage("Booking cancelled. No successful payment found for refund.");
                        }
                } else {
                        response.setRefundStatus(RefundStatus.NONE.name());
                        response.setRefundAmount(BigDecimal.ZERO);
                        response.setMessage("Booking cancelled. Refund is not eligible.");
                }

                bookingRepository.save(booking);
                return response;
        }

        @Override
        @Transactional
        public BookingResponseDto updateVendorBookingStatus(
                        java.util.UUID bookingId,
                        VendorBookingStatusRequestDto request,
                        String currentUserEmail) {
                Booking booking = bookingRepository.findByIdAndDeletedAtIsNull(bookingId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Booking not found."));

                User vendor = userRepository.findByEmailAndDeletedAtIsNull(normalizeEmail(currentUserEmail))
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Vendor not found."));

                if (vendor.getRole() != UserRole.VENDOR) {
                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "Only vendors can update booking status.");
                }

                if (booking.getParkingLocation() == null
                                || booking.getParkingLocation().getVendor() == null
                                || !booking.getParkingLocation().getVendor().getId().equals(vendor.getId())) {
                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "You do not have permission to update this booking.");
                }

                String action = request.getAction().trim().toUpperCase();

                if ("CHECK_IN".equals(action)) {
                        checkInBooking(booking);
                } else if ("COMPLETE".equals(action)) {
                        completeBooking(booking);
                } else {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Invalid action. Allowed values: CHECK_IN, COMPLETE.");
                }

                Booking savedBooking = bookingRepository.save(booking);
                BookingResponseDto response = mapToResponse(savedBooking);
                response.setMessage("Booking status updated successfully.");
                return response;
        }

        private BookingResponseDto mapToResponse(Booking booking) {
                BookingResponseDto dto = new BookingResponseDto();

                dto.setBookingId(booking.getId());

                dto.setDriverId(booking.getDriver().getId());
                dto.setDriverName(booking.getDriver().getName());

                dto.setParkingLocationId(booking.getParkingLocation().getId());
                dto.setParkingLocationName(booking.getParkingLocation().getName());

                dto.setSlotId(booking.getSlot().getId());
                dto.setSlotNumber(booking.getSlot().getSlotNumber());

                if (booking.getVehicleType() != null) {
                        dto.setVehicleType(booking.getVehicleType());
                } else {
                        dto.setVehicleType(booking.getSlot().getVehicleType());
                }

                dto.setStatus(booking.getStatus());
                dto.setStartTime(booking.getStartTime());
                dto.setEndTime(booking.getEndTime());
                dto.setCancelledAt(booking.getCancelledAt());
                dto.setTotalAmount(booking.getTotalAmount());
                dto.setPaymentStatus(PaymentStatus.PENDING);

                paymentRepository.findFirstByBooking_IdAndDeletedAtIsNullOrderByCreatedAtDesc(booking.getId())
                                .ifPresent(payment -> {
                                        dto.setPaymentId(payment.getId());
                                        dto.setPaymentStatus(payment.getStatus());
                                        dto.setPaymentMethod(payment.getPaymentMethod());
                                        dto.setPaidAt(payment.getPaidAt());
                                });

                return dto;
        }

        private void releaseSlotAndIncreaseAvailability(Booking booking) {
                ParkingSlot slot = booking.getSlot();

                if (slot == null) {
                        return;
                }

                slot.setStatus(ParkingSlotStatus.AVAILABLE);
                parkingSlotRepository.save(slot);

                ParkingLocation location = booking.getParkingLocation();

                if (location == null) {
                        return;
                }

                incrementAvailableSlotCount(location, slot.getVehicleType());
                parkingLocationRepository.save(location);
        }

        private void checkInBooking(Booking booking) {
                if (booking.getStatus() != BookingStatus.CONFIRMED) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "Only confirmed bookings can be checked in.");
                }

                ParkingSlot slot = booking.getSlot();

                if (slot == null) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "Booking has no assigned slot.");
                }

                if (slot.getStatus() != ParkingSlotStatus.RESERVED) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "Only reserved slots can be checked in.");
                }

                slot.markOccupied();
                parkingSlotRepository.save(slot);
        }

        private void completeBooking(Booking booking) {
                if (booking.getStatus() != BookingStatus.CONFIRMED) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "Only confirmed bookings can be completed.");
                }

                ParkingSlot slot = booking.getSlot();

                if (slot == null) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "Booking has no assigned slot.");
                }

                if (slot.getStatus() != ParkingSlotStatus.OCCUPIED) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "Only occupied slots can be completed.");
                }

                booking.markCompleted();
                slot.markAvailable();
                parkingSlotRepository.save(slot);

                ParkingLocation location = booking.getParkingLocation();
                if (location != null) {
                        incrementAvailableSlotCount(location, slot.getVehicleType());
                        parkingLocationRepository.save(location);
                }
        }

        private boolean isRefundEligible(Booking booking) {
                return booking.getStartTime() != null
                                && booking.getStartTime().isAfter(LocalDateTime.now().plusHours(1));
        }

        private BigDecimal calculateAmount(
                        ParkingLocation location,
                        VehicleType vehicleType,
                        LocalDateTime startTime,
                        LocalDateTime endTime) {
                long minutes = Duration.between(startTime, endTime).toMinutes();
                long billableHours = Math.max(1, (long) Math.ceil(minutes / 60.0));

                BigDecimal ratePerHour = DEFAULT_HOURLY_RATE;

                if (vehicleType == VehicleType.FOUR_WHEELER && location.getFourWheelerRatePerHour() != null) {
                        ratePerHour = BigDecimal.valueOf(location.getFourWheelerRatePerHour());
                }

                if (vehicleType == VehicleType.TWO_WHEELER && location.getTwoWheelerRatePerHour() != null) {
                        ratePerHour = BigDecimal.valueOf(location.getTwoWheelerRatePerHour());
                }

                return ratePerHour.multiply(BigDecimal.valueOf(billableHours));
        }

        private void validateAvailableSlotCount(ParkingLocation location, VehicleType vehicleType) {
                if (vehicleType == VehicleType.FOUR_WHEELER && location.getAvailableFourWheelerSlots() <= 0) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "No available four-wheeler slots at this location.");
                }

                if (vehicleType == VehicleType.TWO_WHEELER && location.getAvailableTwoWheelerSlots() <= 0) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "No available two-wheeler slots at this location.");
                }
        }

        private void decrementAvailableSlotCount(ParkingLocation location, VehicleType vehicleType) {
                if (vehicleType == VehicleType.FOUR_WHEELER) {
                        location.setAvailableFourWheelerSlots(location.getAvailableFourWheelerSlots() - 1);
                } else {
                        location.setAvailableTwoWheelerSlots(location.getAvailableTwoWheelerSlots() - 1);
                }
        }

        private void incrementAvailableSlotCount(ParkingLocation location, VehicleType vehicleType) {
                if (vehicleType == VehicleType.FOUR_WHEELER) {
                        int current = location.getAvailableFourWheelerSlots();
                        int total = location.getTotalFourWheelerSlots();
                        location.setAvailableFourWheelerSlots(Math.min(total, current + 1));
                } else {
                        int current = location.getAvailableTwoWheelerSlots();
                        int total = location.getTotalTwoWheelerSlots();
                        location.setAvailableTwoWheelerSlots(Math.min(total, current + 1));
                }
        }

        private VehicleType parseVehicleType(String vehicleType) {
                try {
                        return VehicleType.valueOf(vehicleType.trim().toUpperCase());
                } catch (IllegalArgumentException ex) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Invalid vehicle type. Allowed values: TWO_WHEELER, FOUR_WHEELER.");
                }
        }

        private String normalizeEmail(String email) {
                return email.trim().toLowerCase();
        }
}
