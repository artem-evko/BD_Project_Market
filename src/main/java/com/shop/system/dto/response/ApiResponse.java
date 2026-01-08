package com.shop.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class ApiResponse {

    private boolean success;
    private String message;

    public static ApiResponse ok(String message) {
        return ApiResponse.builder()
                .success(true)
                .message(message)
                .build();
    }

    public static ApiResponse fail(String message) {
        return ApiResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}
