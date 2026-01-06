package com.shop.system.controller;

import com.shop.system.dto.request.LoginRequest;
import com.shop.system.dto.response.LoginResponse;
import com.shop.system.dto.response.UserProfileResponse;
import com.shop.system.dto.response.ApiResponse;
import com.shop.system.exception.BusinessException;
import com.shop.system.security.JwtService;
import com.shop.system.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String token = jwtService.resolveFromAuthHeader(authHeader);
        return authService.refresh(token);
    }

    @PostMapping("/logout")
    public ApiResponse logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String token = jwtService.resolveFromAuthHeader(authHeader);
        return authService.logout(token);
    }

    @GetMapping("/me")
    public UserProfileResponse getProfile() {
        // токен уже распарсен фильтром, тут работаем только с SecurityContext
        return authService.getProfile();
    }

}
