package com.dataflow.ai.api.controller;

import com.dataflow.ai.domain.request.LoginRequest;
import com.dataflow.ai.domain.response.LoginResponse;
import com.dataflow.ai.business.service.UserService;
import com.dataflow.ai.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "认证", description = "用户认证相关接口")
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request: username={}", request.getUsername());
        LoginResponse response = userService.login(request);
        return ApiResponse.ofSuccess(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出")
    public ApiResponse<Void> logout() {
        // JWT是无状态的，登出主要是在客户端清除token
        log.info("User logout");
        return ApiResponse.ofSuccess();
    }
}
