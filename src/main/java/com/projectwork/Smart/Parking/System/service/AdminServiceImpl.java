package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.response.BookingResponseDto;
import com.projectwork.Smart.Parking.System.dto.response.UserResponseDto;
import com.projectwork.Smart.Parking.System.entity.Booking;
import com.projectwork.Smart.Parking.System.entity.User;
import com.projectwork.Smart.Parking.System.entity.UserRole;
import com.projectwork.Smart.Parking.System.repository.BookingRepository;
import com.projectwork.Smart.Parking.System.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminServiceImpl implements AdminService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    public AdminServiceImpl(
            BookingRepository bookingRepository,
            UserRepository userRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
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

    private UserResponseDto toUserResponse(User user) {
        UserResponseDto dto = new UserResponseDto();

        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getRole());

        return dto;
    }

    private BookingResponseDto toBookingResponse(Booking booking) {
        BookingResponseDto dto = new BookingResponseDto();

        dto.setBookingId(booking.getId());

        if (booking.getDriver() != null) {
            dto.setDriverId(booking.getDriver().getId());
            dto.setDriverName(booking.getDriver().getName());
        }

        if (booking.getParkingLocation() != null) {
            dto.setParkingLocationId(booking.getParkingLocation().getId());
            dto.setParkingLocationName(booking.getParkingLocation().getName());
        }

        if (booking.getSlot() != null) {
            dto.setSlotId(booking.getSlot().getId());
            dto.setSlotNumber(booking.getSlot().getSlotNumber());
            dto.setVehicleType(booking.getSlot().getVehicleType());
        }

        dto.setStatus(booking.getStatus());
        dto.setStartTime(booking.getStartTime());
        dto.setEndTime(booking.getEndTime());
        dto.setTotalAmount(booking.getTotalAmount());

        return dto;
    }
}
