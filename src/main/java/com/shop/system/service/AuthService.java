package com.shop.system.service;

import com.shop.system.domain.entity.UserAccount;
import com.shop.system.dto.request.LoginRequest;
import com.shop.system.dto.response.ApiResponse;
import com.shop.system.dto.response.LoginResponse;
import com.shop.system.dto.response.UserProfileResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    ApiResponse logout(String token);

    LoginResponse refresh(String token);

    UserProfileResponse getProfile();
}
