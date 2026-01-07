package com.shop.system.dto.request;

import com.shop.system.dto.PassportDataDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class UpdateEmployeeRequest {

    @NotBlank
    private String fullName;

    @NotNull
    private UUID positionId;

    @NotNull
    private UUID departmentId;

    @NotBlank
    private String roleCode;

    @NotBlank
    private String workPhone;

    @NotBlank
    private String personalPhone;

    @Email
    private String email;

    @NotNull
    @PastOrPresent(message = "Дата приема не может быть в будущем")
    private LocalDate employmentDate;

    @NotNull
    @Valid
    private PassportDataDto passportData;
}
