package com.shop.system.dto.response;

import com.shop.system.dto.PassportDataDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class EmployeeDetailsResponse {

    private UUID id;
    private String fullName;
    private String status;

    private String position;
    private String department;
    private String role;

    private String workPhone;
    private String personalPhone;
    private String email;

    private LocalDate employmentDate;
    private LocalDate terminationDate;

    private PassportDataDto passportData;
}
