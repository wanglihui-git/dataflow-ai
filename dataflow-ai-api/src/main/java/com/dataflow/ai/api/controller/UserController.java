package com.dataflow.ai.api.controller;

import com.dataflow.ai.business.service.UserService;
import com.dataflow.ai.domain.response.ApiResponse;
import com.dataflow.ai.common.utils.SecurityUtils;
import com.dataflow.ai.domain.entity.User;
import com.dataflow.ai.domain.mapper.UserMapper;
import com.dataflow.ai.domain.request.ChangePasswordRequest;
import com.dataflow.ai.domain.request.CreateUserRequest;
import com.dataflow.ai.domain.vo.UserVO;
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
    public ApiResponse<List<UserVO>> list() {
        return ApiResponse.ofSuccess(UserMapper.toVOList(userService.findAllUsers()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询用户详情")
    public ApiResponse<UserVO> get(@PathVariable String id) {
        User user = userService.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return ApiResponse.ofSuccess(UserMapper.toVO(user));
    }

    @PutMapping("/me/password")
    @Operation(summary = "修改当前用户密码")
    public ApiResponse<Void> changeMyPassword(@Valid @RequestBody ChangePasswordRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return ApiResponse.ofSuccess();
    }

    @PostMapping
    @Operation(summary = "创建用户")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserVO> create(@RequestBody @Valid CreateUserRequest request) {
        log.info("Creating user: {}", request.getUsername());
        User user = userService.createUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getRole(),
                request.getDepartment()
        );
        return ApiResponse.ofSuccess(UserMapper.toVO(user));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新用户")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserVO> update(@PathVariable String id, @RequestBody User user) {
        user.setId(id);
        User updated = userService.updateUser(user);
        return ApiResponse.ofSuccess(UserMapper.toVO(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        userService.deleteUser(id);
        return ApiResponse.ofSuccess();
    }
}
