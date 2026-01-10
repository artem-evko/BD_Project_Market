package com.shop.system.controller;

import com.shop.system.dto.request.GenerateScheduleFromTemplateRequest;
import com.shop.system.dto.request.UpdateScheduleRequest;
import com.shop.system.dto.response.ApiResponse;
import com.shop.system.dto.response.ScheduleItemResponse;
import com.shop.system.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping
    public List<ScheduleItemResponse> list(
            @RequestParam LocalDate dateFrom,
            @RequestParam LocalDate dateTo,
            @RequestParam(required = false) UUID employeeId
    ) {
        return scheduleService.list(dateFrom, dateTo, employeeId);
    }

    @PostMapping("/generate-from-template")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    public ApiResponse generateFromTemplate(@RequestBody @Valid GenerateScheduleFromTemplateRequest request) {
        return scheduleService.generateFromTemplate(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    public ScheduleItemResponse update(@PathVariable UUID id, @RequestBody @Valid UpdateScheduleRequest request) {
        return scheduleService.update(id, request);
    }
}
