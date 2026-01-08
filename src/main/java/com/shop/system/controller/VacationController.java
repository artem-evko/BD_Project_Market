package com.shop.system.controller;

import com.shop.system.dto.request.CreateVacationRequest;
import com.shop.system.dto.request.RejectRequest;
import com.shop.system.dto.response.ApiResponse;
import com.shop.system.dto.response.VacationListItemResponse;
import com.shop.system.service.VacationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/vacations")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()") // настрой под свои роли
public class VacationController {

    private final VacationService vacationService;

    @GetMapping
    public Page<VacationListItemResponse> list(
            @RequestParam(value = "employeeId", required = false) UUID employeeId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "dateFrom", required = false) LocalDate dateFrom,
            @RequestParam(value = "dateTo", required = false) LocalDate dateTo,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return vacationService.list(employeeId, status, dateFrom, dateTo, page, size);
    }

    @PostMapping
    public ApiResponse create(@RequestBody @Valid CreateVacationRequest request) {
        return vacationService.create(request);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    public ApiResponse approve(@PathVariable UUID id) {
        return vacationService.approve(id);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    public ApiResponse reject(@PathVariable UUID id, @RequestBody @Valid RejectRequest request) {
        return vacationService.reject(id, request);
    }
}
