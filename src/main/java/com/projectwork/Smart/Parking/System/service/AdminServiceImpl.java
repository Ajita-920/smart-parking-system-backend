package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.response.BookingResponseDto;
import com.projectwork.Smart.Parking.System.dto.response.UserResponseDto;
import com.projectwork.Smart.Parking.System.entity.Booking;
import com.projectwork.Smart.Parking.System.entity.ParkingLocation;
import com.projectwork.Smart.Parking.System.entity.Payment;
import com.projectwork.Smart.Parking.System.entity.PaymentStatus;
import com.projectwork.Smart.Parking.System.entity.User;
import com.projectwork.Smart.Parking.System.entity.UserRole;
import com.projectwork.Smart.Parking.System.repository.BookingRepository;
import com.projectwork.Smart.Parking.System.repository.ParkingLocationRepository;
import com.projectwork.Smart.Parking.System.repository.PaymentRepository;
import com.projectwork.Smart.Parking.System.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implements admin workflows against active, non-soft-deleted records.
 */
@Service
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ParkingLocationRepository parkingLocationRepository;
    private final PaymentRepository paymentRepository;
    private final EmailService emailService;

    public AdminServiceImpl(
            BookingRepository bookingRepository,
            UserRepository userRepository,
            ParkingLocationRepository parkingLocationRepository,
            PaymentRepository paymentRepository,
            EmailService emailService
    ) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.parkingLocationRepository = parkingLocationRepository;
        this.paymentRepository = paymentRepository;
        this.emailService = emailService;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboard() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalBookings", bookingRepository.countByDeletedAtIsNull());
        stats.put("totalVendors", userRepository.countByRoleAndDeletedAtIsNull(UserRole.VENDOR));
        stats.put("totalDrivers", userRepository.countByRoleAndDeletedAtIsNull(UserRole.DRIVER));
        stats.put("totalAdmins", userRepository.countByRoleAndDeletedAtIsNull(UserRole.ADMIN));

        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getAllBookings() {
        return bookingRepository.findByDeletedAtIsNull()
                .stream()
                .map(this::toBookingResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> getUsers(String role) {
        List<User> users;

        if (role == null || role.isBlank()) {
            users = userRepository.findByDeletedAtIsNull();
        } else {
            UserRole userRole = parseUserRole(role);
            users = userRepository.findByRoleAndDeletedAtIsNull(userRole);
        }

        return users.stream()
                .map(this::toUserResponse)
                .toList();
    }

    @Override
    @Transactional
    public UserResponseDto banUser(UUID id) {
        User user = resolveActiveUser(id);

        rejectAdminModeration(user, "banned");

        user.setBanned(true);
        return toUserResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponseDto unbanUser(UUID id) {
        User user = resolveActiveUser(id);

        rejectAdminModeration(user, "unbanned");

        user.setBanned(false);
        return toUserResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        User user = resolveActiveUser(id);

        if (user.getRole() == UserRole.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Admin users cannot be deleted from this endpoint."
            );
        }

        if (user.getRole() == UserRole.VENDOR) {
            softDeleteVendorParkingLocations(user);
        }

        user.softDelete();
        userRepository.save(user);
    }

    @Override
    @Transactional
    public UserResponseDto approveVendor(UUID id) {
        User vendor = resolveActiveUser(id);

        if (vendor.getRole() != UserRole.VENDOR) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only vendors can be approved."
            );
        }

        vendor.setApproved(true);
        User savedVendor = userRepository.save(vendor);

        log.info("Vendor {} approved; attempting approval email to '{}'", savedVendor.getId(), savedVendor.getEmail());
        try {
            emailService.sendVendorApprovalEmail(savedVendor);
            log.info("Triggered vendor approval email for vendor {}", savedVendor.getId());
        } catch (Exception e) {
            log.error("Could not trigger vendor approval email for vendor {}", savedVendor.getId(), e);
        }

        return toUserResponse(savedVendor);
    }

    @Override
    @Transactional
    public void deleteVendor(UUID id) {
        User vendor = resolveActiveUser(id);

        if (vendor.getRole() != UserRole.VENDOR) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only vendors can be deleted from this endpoint."
            );
        }

        softDeleteVendorParkingLocations(vendor);
        vendor.softDelete();
        userRepository.save(vendor);
    }

    /**
     * Soft-deletes every active parking location owned by a vendor.
     */
    private void softDeleteVendorParkingLocations(User vendor) {
        List<ParkingLocation> parkingLocations = parkingLocationRepository.findByVendorAndDeletedAtIsNull(vendor);

        parkingLocations.forEach(ParkingLocation::softDelete);
        parkingLocationRepository.saveAll(parkingLocations);
    }

    /**
     * Fetches a user while excluding soft-deleted records.
     */
    private User resolveActiveUser(UUID id) {
        return userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found."
                ));
    }

    /**
     * Prevents admin accounts from being moderated through user ban endpoints.
     */
    private void rejectAdminModeration(User user, String action) {
        if (user.getRole() == UserRole.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Admin users cannot be " + action + "."
            );
        }
    }

    /**
     * Parses the optional user-role filter accepted by admin endpoints.
     */
    private UserRole parseUserRole(String role) {
        try {
            return UserRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid role filter. Accepted values: ADMIN, VENDOR, DRIVER."
            );
        }
    }

    /**
     * Maps a user entity into the admin user response shape.
     */
    private UserResponseDto toUserResponse(User user) {
        UserResponseDto dto = new UserResponseDto();

        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getRole());
        dto.setBanned(user.isBanned());
        dto.setApproved(user.isApproved());
        dto.setJoinedAt(user.getCreatedAt() != null ? user.getCreatedAt() : user.getUpdatedAt());
        dto.setCreatedAt(dto.getJoinedAt());

        return dto;
    }

    /**
     * Maps booking relationships defensively because some relations can be absent
     * after soft-delete or partial data creation.
     */
    private BookingResponseDto toBookingResponse(Booking booking) {
        BookingResponseDto dto = new BookingResponseDto();

        dto.setBookingId(booking.getId());

        if (booking.getDriver() != null) {
            dto.setDriverId(booking.getDriver().getId());
            dto.setDriverName(booking.getDriver().getName());
        }

        dto.setCustomerName(booking.getCustomerName());
        dto.setCustomerPhone(booking.getCustomerPhone());
        dto.setVehicleNumber(booking.getVehicleNumber());
        dto.setWalkIn(booking.isWalkIn());

        if (booking.getParkingLocation() != null) {
            dto.setParkingLocationId(booking.getParkingLocation().getId());
            dto.setParkingLocationName(booking.getParkingLocation().getName());
            if (booking.getParkingLocation().getVendor() != null) {
                dto.setVendorId(booking.getParkingLocation().getVendor().getId());
                dto.setVendorName(booking.getParkingLocation().getVendor().getName());
            }
        }

        if (booking.getSlot() != null) {
            dto.setSlotId(booking.getSlot().getId());
            dto.setSlotNumber(booking.getSlot().getSlotNumber());
            dto.setSlotStatus(booking.getSlot().getStatus());
            dto.setVehicleType(booking.getSlot().getVehicleType());
        }

        dto.setStatus(booking.getStatus());
        dto.setStartTime(booking.getStartTime());
        dto.setEndTime(booking.getEndTime());
        dto.setCreatedAt(booking.getCreatedAt());
        dto.setCancelledAt(booking.getCancelledAt());
        dto.setTotalAmount(booking.getTotalAmount());

        paymentRepository.findFirstByBooking_IdAndStatusAndDeletedAtIsNullOrderByPaidAtDesc(
                        booking.getId(),
                        PaymentStatus.SUCCESS)
                .or(() -> paymentRepository.findFirstByBooking_IdAndDeletedAtIsNullOrderByCreatedAtDesc(
                        booking.getId()))
                .ifPresent(payment -> applyPayment(dto, payment));

        return dto;
    }

    private void applyPayment(BookingResponseDto dto, Payment payment) {
        dto.setPaymentId(payment.getId());
        dto.setPaymentStatus(payment.getStatus());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setPaidAt(payment.getPaidAt());
    }
}
