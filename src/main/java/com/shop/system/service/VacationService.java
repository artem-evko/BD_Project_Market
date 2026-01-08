package com.shop.system.service;

import com.shop.system.dto.request.CreateVacationRequest;
import com.shop.system.dto.request.RejectRequest;
import com.shop.system.dto.response.ApiResponse;
import com.shop.system.dto.response.VacationListItemResponse;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.UUID;

public interface VacationService {

    Page<VacationListItemResponse> list(
            UUID employeeId,
            String status,
            LocalDate dateFrom,
            LocalDate dateTo,
            int page,
            int size
    );

    ApiResponse create(CreateVacationRequest request);

    ApiResponse approve(UUID id);

    ApiResponse reject(UUID id, RejectRequest request);
}
