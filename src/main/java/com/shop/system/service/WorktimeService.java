package com.shop.system.service;

import com.shop.system.dto.request.ConfirmWorktimeRequest;
import com.shop.system.dto.response.ApiResponse;
import com.shop.system.dto.response.OpenWorktimeResponse;
import com.shop.system.dto.response.WorktimeListItemResponse;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.UUID;

public interface WorktimeService {

    Page<WorktimeListItemResponse> list(UUID employeeId, LocalDate dateFrom, LocalDate dateTo, int page, int size);

    OpenWorktimeResponse open();

    ApiResponse confirm(UUID worktimeId, ConfirmWorktimeRequest request);
}
