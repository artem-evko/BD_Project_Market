package com.shop.system.service;

import com.shop.system.dto.request.CreateEmployeeRequest;
import com.shop.system.dto.request.UpdateEmployeeRequest;
import com.shop.system.dto.response.ApiResponse;
import com.shop.system.dto.response.EmployeeDetailsResponse;
import com.shop.system.dto.response.EmployeeListItemResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface EmployeeService {

    Page<EmployeeListItemResponse> getEmployees(String search,
                                                UUID departmentId,
                                                UUID positionId,
                                                String status,
                                                int page,
                                                int size);

    EmployeeDetailsResponse getEmployee(UUID id);

    ApiResponse createEmployee(CreateEmployeeRequest request);

    ApiResponse updateEmployee(UUID id, UpdateEmployeeRequest request);

    ApiResponse terminateEmployee(UUID id);

    ApiResponse deleteEmployee(UUID id);
}
