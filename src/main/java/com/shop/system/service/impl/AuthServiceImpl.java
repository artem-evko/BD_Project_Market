package com.shop.system.service.impl;

import com.shop.system.domain.entity.UserAccount;
import com.shop.system.dto.request.LoginRequest;
import com.shop.system.dto.response.LoginResponse;
import com.shop.system.dto.response.UserProfileResponse;
import com.shop.system.dto.response.ApiResponse;
import com.shop.system.exception.BusinessException;
import com.shop.system.repository.UserAccountRepository;
import com.shop.system.security.CurrentUserPrincipal;
import com.shop.system.security.JwtService;
import com.shop.system.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        UserAccount user = userAccountRepository
                .findActiveWithDetails(request.getLogin())
                .orElseThrow(() -> new BusinessException("Неверный логин или пароль"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("Неверный логин или пароль");
        }

        String token = jwtService.generateToken(user);
        Instant expiresAt = jwtService.getExpiration(token);

        var employee = user.getEmployee();
        var role = user.getRole();

        return new LoginResponse(
                token,
                employee.getId(),
                employee.getFullName(),
                role.getName(),
                expiresAt
        );
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse refresh(String token) {
        if (!jwtService.isTokenValid(token)) {
            throw new BusinessException("Невалидный токен");
        }

        String login = jwtService.extractUsername(token);

        UserAccount user = userAccountRepository
                .findActiveWithDetails(login)
                .orElseThrow(() -> new BusinessException("Пользователь не найден"));

        String newToken = jwtService.generateToken(user);
        Instant expiresAt = jwtService.getExpiration(newToken);

        var employee = user.getEmployee();
        var role = user.getRole();

        return new LoginResponse(
                newToken,
                employee.getId(),
                employee.getFullName(),
                role.getName(),
                expiresAt
        );
    }

    @Override
    public ApiResponse logout(String token) {
        // Никакой ревокации  нет, просто успешный ответ
        return new ApiResponse(true, "Logged out");
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CurrentUserPrincipal principal)) {
            throw new BusinessException("Пользователь не аутентифицирован");
        }

        String login = principal.login();

        UserAccount user = userAccountRepository
                .findActiveWithDetails(login)
                .orElseThrow(() -> new BusinessException("Пользователь не найден"));

        var employee = user.getEmployee();
        var role = user.getRole();

        return new UserProfileResponse(
                employee.getId(),
                employee.getFullName(),
                employee.getPosition() != null ? employee.getPosition().getName() : null,
                employee.getDepartment() != null ? employee.getDepartment().getName() : null,
                role.getName()
        );
    }
}
