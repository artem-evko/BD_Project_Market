package com.shop.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class EmployeeListItemResponse {

    private UUID id;
    private String fullName;
    private String position;
    private String department;
    private String role;
    private String status; // active/terminated/vacation/sick
}
