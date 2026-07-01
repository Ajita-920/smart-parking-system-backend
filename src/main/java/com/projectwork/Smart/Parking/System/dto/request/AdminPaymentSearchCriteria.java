package com.projectwork.Smart.Parking.System.dto.request;

import com.projectwork.Smart.Parking.System.entity.PaymentMethod;
import com.projectwork.Smart.Parking.System.entity.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class AdminPaymentSearchCriteria {
    private String search;
    private PaymentStatus status;
    private PaymentMethod method;
    private LocalDate fromDate;
    private LocalDate toDate;
}
