package com.dataflow.ai.api.controller;

import com.dataflow.ai.domain.request.LoginRequest;
import com.dataflow.ai.domain.request.RefreshTokenRequest;
import com.dataflow.ai.domain.response.LoginResponse;
import com.dataflow.ai.business.service.UserService;
import com.dataflow.ai.domain.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证 REST 控制器。
 * <p>
 * 提供登录、刷新令牌与登出接口；JWT 无状态，登出由客户端清除本地令牌。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "认证", description = "用户认证相关接口")
public class AuthController {

    private final UserService userService;

    /**
     * 用户登录，校验凭据后颁发访问令牌与刷新令牌。
     *
     * @param request 用户名与密码（{@link LoginRequest}）
     * @return 统一响应，data 为 {@link LoginResponse}
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request: username={}", request.getUsername());
        LoginResponse response = userService.login(request);
        return ApiResponse.ofSuccess(response);
    }

    /**
     * 使用 refreshToken 换取新的访问令牌与刷新令牌。
     *
     * @param request 刷新令牌
     * @return 新的 {@link LoginResponse}
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新访问令牌")
    public ApiResponse<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = userService.refreshToken(request.getRefreshToken());
        return ApiResponse.ofSuccess(response);
    }

    /**
     * 登出。服务端不吊销 JWT，客户端应删除本地 token。
     *
     * @return 空 data 的成功响应
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出")
    public ApiResponse<Void> logout() {
        log.info("User logout");
        return ApiResponse.ofSuccess();
    }
}
