package com.shop.system.dto.request;

import com.shop.system.dto.PassportDataDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateEmployeeRequest {

    @NotBlank
    private String fullName;

    @NotNull
    private UUID positionId;

    @NotNull
    private UUID departmentId;

    @NotBlank
    @Pattern(regexp = "^[a-z0-9_]{3,50}$", message = "Логин: латиница/цифры/_ , 3-50 символов")
    private String login;

    @NotBlank
    @Size(min = 8, message = "Пароль минимум 8 символов")
    private String password;

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
