package com.projectwork.Smart.Parking.System.service;

import com.projectwork.Smart.Parking.System.dto.request.AdminPaymentSearchCriteria;
import com.projectwork.Smart.Parking.System.dto.response.AdminPaymentDetailResponseDto;
import com.projectwork.Smart.Parking.System.dto.response.AdminPaymentPageResponseDto;
import com.projectwork.Smart.Parking.System.dto.response.AdminPaymentSummaryResponseDto;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminPaymentService {
    AdminPaymentSummaryResponseDto getPaymentSummary(AdminPaymentSearchCriteria criteria);

    AdminPaymentPageResponseDto getPayments(AdminPaymentSearchCriteria criteria, Pageable pageable);

    AdminPaymentDetailResponseDto getPaymentDetail(UUID paymentId);
}
