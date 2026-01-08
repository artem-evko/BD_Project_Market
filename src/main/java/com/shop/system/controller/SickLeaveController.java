package com.shop.system.controller;

import com.shop.system.dto.request.CreateSickLeaveRequest;
import com.shop.system.dto.request.RejectRequest;
import com.shop.system.dto.response.ApiResponse;
import com.shop.system.dto.response.SickLeaveListItemResponse;
import com.shop.system.service.SickLeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/sick-leaves")
@RequiredArgsConstructor
public class SickLeaveController {

    private final SickLeaveService sickLeaveService;

    /**
     * GET /api/sick-leaves?employeeId=&status=&docStatus=&dateFrom=&dateTo=&page=&size=
     *
     * Важно: employeeId игнорируется на бэке, если роль не DIRECTOR/ADMIN (будет "мой режим").
     */
    @GetMapping
    public Page<SickLeaveListItemResponse> list(
            @RequestParam(value = "employeeId", required = false) UUID employeeId,
            @RequestParam(value = "status", required = false) String status,         // pending/approved/rejected
            @RequestParam(value = "docStatus", required = false) String docStatus,   // pending/received/overdue
            @RequestParam(value = "dateFrom", required = false) LocalDate dateFrom,
            @RequestParam(value = "dateTo", required = false) LocalDate dateTo,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return sickLeaveService.list(employeeId, status, docStatus, dateFrom, dateTo, page, size);
    }

    /**
     * Создать больничный (любой авторизованный сотрудник).
     */
    @PostMapping
    public ApiResponse create(@RequestBody @Valid CreateSickLeaveRequest request) {
        return sickLeaveService.create(request);
    }

    /**
     * Подписать (director/admin)
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    public ApiResponse approve(@PathVariable UUID id) {
        return sickLeaveService.approve(id);
    }

    /**
     * Отклонить (director/admin), rejectComment обязателен
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")
    public ApiResponse reject(@PathVariable UUID id, @RequestBody @Valid RejectRequest request) {
        return sickLeaveService.reject(id, request);
    }

    /**
     * Загрузка документа (только владелец записи)
     * multipart/form-data: file
     */
    @PostMapping("/{id}/document")
    public ApiResponse upload(@PathVariable UUID id, @RequestPart("file") MultipartFile file) {
        return sickLeaveService.uploadDocument(id, file);
    }
}
