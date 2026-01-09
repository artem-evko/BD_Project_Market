package com.shop.system.service;

import com.shop.system.dto.request.ConfirmWorkReportRequest;
import com.shop.system.dto.request.GenerateWorkReportRequest;
import com.shop.system.dto.response.*;

import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.UUID;

public interface WorkReportService {

    Page<WorkReportHeaderListItemResponse> list(LocalDate weekStart, String status, int page, int size);

    WorkReportDetailsResponse get(UUID id);

    ApiResponse generate(GenerateWorkReportRequest request);

    ApiResponse confirm(UUID id, ConfirmWorkReportRequest request);

    ApiResponse send(UUID id);
}
