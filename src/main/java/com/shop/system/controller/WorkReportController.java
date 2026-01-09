package com.shop.system.controller;

import com.shop.system.dto.request.ConfirmWorkReportRequest;
import com.shop.system.dto.request.GenerateWorkReportRequest;
import com.shop.system.dto.response.*;
import com.shop.system.service.WorkReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/work-reports")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class WorkReportController {

    private final WorkReportService workReportService;

    /**
     * GET /api/work-reports?weekStart=YYYY-MM-DD&status=all|draft|confirmed|sent&page=0&size=20
     */
    @GetMapping
    public Page<WorkReportHeaderListItemResponse> list(
            @RequestParam(value = "weekStart", required = false) LocalDate weekStart,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return workReportService.list(weekStart, status, page, size);
    }

    /**
     * GET /api/work-reports/{id}
     */
    @GetMapping("/{id}")
    public WorkReportDetailsResponse get(@PathVariable UUID id) {
        return workReportService.get(id);
    }

    /**
     * POST /api/work-reports/generate (DIRECTOR)
     * body: { "weekStart":"2026-01-06" }
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    public ApiResponse generate(@RequestBody @Valid GenerateWorkReportRequest request) {
        return workReportService.generate(request);
    }

    /**
     * POST /api/work-reports/{id}/confirm (DIRECTOR)
     */
    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    public ApiResponse confirm(@PathVariable UUID id, @RequestBody @Valid ConfirmWorkReportRequest request) {
        return workReportService.confirm(id, request);
    }

    /**
     * POST /api/work-reports/{id}/send (DIRECTOR)
     */
    @PostMapping("/{id}/send")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    public ApiResponse send(@PathVariable UUID id) {
        return workReportService.send(id);
    }
}
