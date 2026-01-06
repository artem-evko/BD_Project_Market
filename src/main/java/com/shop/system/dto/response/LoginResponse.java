package com.shop.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private UUID employeeId;
    private String fullName;
    private String role;
    private Instant expiresAt;
}
