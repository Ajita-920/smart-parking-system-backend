package com.projectwork.Smart.Parking.System.dto.response;

import com.projectwork.Smart.Parking.System.entity.PaymentMethod;
import com.projectwork.Smart.Parking.System.entity.PaymentStatus;
import com.projectwork.Smart.Parking.System.entity.UserRole;
import com.projectwork.Smart.Parking.System.entity.VehicleType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AdminPaymentListItemResponseDto {
    private UUID paymentId;
    private UUID bookingId;
    private String customerName;
    private String customerPhone;
    private UserRole customerRole;
    private String parkingLocationName;
    private String slotCode;
    private String vehicleNumber;
    private VehicleType vehicleType;
    private BigDecimal amount;
    private PaymentMethod method;
    private PaymentStatus status;
    private String transactionId;
    private String khaltiPidx;
    private Instant paidAt;
    private LocalDateTime bookingStartTime;
    private LocalDateTime bookingEndTime;
    private String bookingSource;
}
