package com.projectwork.Smart.Parking.System.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class AdminPaymentPageResponseDto {
    private List<AdminPaymentListItemResponseDto> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
