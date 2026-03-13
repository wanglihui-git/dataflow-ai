package com.dataflow.ai.api.controller;

import com.dataflow.ai.business.service.UserService;
import com.dataflow.ai.domain.response.ApiResponse;
import com.dataflow.ai.domain.entity.User;
import com.dataflow.ai.domain.request.CreateUserRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Tag(name = "用户", description = "用户管理相关接口")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "查询用户列表")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<User>> list() {
        List<User> users = userService.findAllUsers();
        return ApiResponse.ofSuccess(users);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询用户详情")
    public ApiResponse<User> get(@PathVariable String id) {
        User user = userService.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return ApiResponse.ofSuccess(user);
    }

    @PostMapping
    @Operation(summary = "创建用户")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<User> create(@RequestBody @Valid CreateUserRequest request) {
        log.info("Creating user: {}", request.getUsername());
        User user = userService.createUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getRole(),
                request.getDepartment()
        );
        return ApiResponse.ofSuccess(user);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新用户")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<User> update(@PathVariable String id, @RequestBody User user) {
        user.setId(id);
        User updated = userService.updateUser(user);
        return ApiResponse.ofSuccess(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        userService.deleteUser(id);
        return ApiResponse.ofSuccess();
    }
}
