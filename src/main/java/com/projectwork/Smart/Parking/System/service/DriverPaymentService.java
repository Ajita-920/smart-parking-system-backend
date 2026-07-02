package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.request.DriverPaymentSearchCriteria;
import com.projectwork.Smart.Parking.System.dto.response.DriverPaymentHistoryItemResponseDto;
import com.projectwork.Smart.Parking.System.dto.response.DriverPaymentPageResponseDto;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface DriverPaymentService {
    DriverPaymentPageResponseDto getPayments(String currentUserEmail, DriverPaymentSearchCriteria criteria, Pageable pageable);

    DriverPaymentHistoryItemResponseDto getPaymentDetail(String currentUserEmail, UUID paymentId);
}
