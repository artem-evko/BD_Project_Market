package com.shop.system.service;

import com.shop.system.dto.request.GenerateScheduleFromTemplateRequest;
import com.shop.system.dto.request.UpdateScheduleRequest;
import com.shop.system.dto.response.ApiResponse;
import com.shop.system.dto.response.ScheduleItemResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ScheduleService {

    List<ScheduleItemResponse> list(LocalDate dateFrom, LocalDate dateTo, UUID employeeId);

    ApiResponse generateFromTemplate(GenerateScheduleFromTemplateRequest request);

    ScheduleItemResponse update(UUID id, UpdateScheduleRequest request);
}
