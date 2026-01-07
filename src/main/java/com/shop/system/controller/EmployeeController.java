package com.shop.system.controller;

import com.shop.system.dto.request.CreateEmployeeRequest;
import com.shop.system.dto.request.UpdateEmployeeRequest;
import com.shop.system.dto.response.ApiResponse;
import com.shop.system.dto.response.EmployeeDetailsResponse;
import com.shop.system.dto.response.EmployeeListItemResponse;
import com.shop.system.dto.response.PageResponse;
import com.shop.system.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('DIRECTOR','ADMIN')")

public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public PageResponse<EmployeeListItemResponse> getEmployees(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "departmentId", required = false) UUID departmentId,
            @RequestParam(value = "positionId", required = false) UUID positionId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        Page<EmployeeListItemResponse> result =
                employeeService.getEmployees(search, departmentId, positionId, status, page, size);

        return PageResponse.<EmployeeListItemResponse>builder()
                .content(result.getContent())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    @GetMapping("/{id}")
    public EmployeeDetailsResponse getEmployee(@PathVariable UUID id) {
        return employeeService.getEmployee(id);
    }

    @PostMapping
    public ApiResponse createEmployee(@RequestBody @Valid CreateEmployeeRequest request) {
        return employeeService.createEmployee(request);
    }

    @PutMapping("/{id}")
    public ApiResponse updateEmployee(@PathVariable UUID id,
                                      @RequestBody @Valid UpdateEmployeeRequest request) {
        return employeeService.updateEmployee(id, request);
    }

    @PatchMapping("/{id}/terminate")
    public ApiResponse terminateEmployee(@PathVariable UUID id) {
        return employeeService.terminateEmployee(id);
    }

    //todo Если будет связь с другими таблицами ту будет ошибка при удалении можно сделать так
    // employment_status = archived
    // user_accounts.is_active=false
    // вместо удаления, либо при демонстрации удалять только ребят без связей

    @DeleteMapping("/{id}")
    public ApiResponse deleteEmployee(@PathVariable UUID id) {
        return employeeService.deleteEmployee(id);
    }
}
