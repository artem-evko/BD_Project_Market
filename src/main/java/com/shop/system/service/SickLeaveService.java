package com.shop.system.service;

import com.shop.system.dto.request.CreateSickLeaveRequest;
import com.shop.system.dto.request.RejectRequest;
import com.shop.system.dto.response.ApiResponse;
import com.shop.system.dto.response.SickLeaveListItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.UUID;

public interface SickLeaveService {

    Page<SickLeaveListItemResponse> list(
            UUID employeeId,
            String status,
            String docStatus,
            LocalDate dateFrom,
            LocalDate dateTo,
            int page,
            int size
    );

    ApiResponse create(CreateSickLeaveRequest request);

    ApiResponse approve(UUID id);

    ApiResponse reject(UUID id, RejectRequest request);

    ApiResponse uploadDocument(UUID id, MultipartFile file);
}
