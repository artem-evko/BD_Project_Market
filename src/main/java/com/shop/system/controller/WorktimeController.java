package com.shop.system.controller;

import com.shop.system.dto.request.ConfirmWorktimeRequest;
import com.shop.system.dto.response.ApiResponse;
import com.shop.system.dto.response.OpenWorktimeResponse;
import com.shop.system.dto.response.WorktimeListItemResponse;
import com.shop.system.service.WorktimeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/worktime")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class WorktimeController {

    private final WorktimeService worktimeService;

    @GetMapping
    public Page<WorktimeListItemResponse> list(
            @RequestParam(value = "employeeId", required = false) UUID employeeId,

            @RequestParam(value = "dateFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateFrom,

            @RequestParam(value = "dateTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateTo,

            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return worktimeService.list(employeeId, dateFrom, dateTo, page, size);
    }

    @GetMapping("/open")
    public OpenWorktimeResponse open() {
        return worktimeService.open();
    }

    @PostMapping("/{worktimeId}/confirm")
    public ApiResponse confirm(@PathVariable UUID worktimeId,
                               @RequestBody @Valid ConfirmWorktimeRequest request) {
        return worktimeService.confirm(worktimeId, request);
    }
}
