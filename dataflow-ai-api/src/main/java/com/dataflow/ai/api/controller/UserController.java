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
 * 用户管理 REST 控制器。
 * <p>
 * 管理员可 CRUD 用户；任意已登录用户可查询详情（按 ID）并修改自己的密码。
 * 对外返回 {@link UserVO}，不包含密码哈希。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Tag(name = "用户", description = "用户管理相关接口")
public class UserController {

    private final UserService userService;

    /**
     * 查询全部用户列表（仅管理员）。
     *
     * @return 用户 VO 列表
     */
    @GetMapping
    @Operation(summary = "查询用户列表")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserVO>> list() {
        return ApiResponse.ofSuccess(UserMapper.toVOList(userService.findAllUsers()));
    }

    /**
     * 按 ID 查询用户详情。
     *
     * @param id 用户 ID
     * @return 用户 VO；不存在时抛运行时异常（由全局异常处理）
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询用户详情")
    public ApiResponse<UserVO> get(@PathVariable String id) {
        User user = userService.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return ApiResponse.ofSuccess(UserMapper.toVO(user));
    }

    /**
     * 修改当前登录用户的密码。
     *
     * @param request 原密码与新密码
     * @return 成功时 data 为 null
     */
    @PutMapping("/me/password")
    @Operation(summary = "修改当前用户密码")
    public ApiResponse<Void> changeMyPassword(@Valid @RequestBody ChangePasswordRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return ApiResponse.ofSuccess();
    }

    /**
     * 创建用户（仅管理员）。
     *
     * @param request 用户名、邮箱、密码、角色等
     * @return 创建后的用户 VO
     */
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

    /**
     * 更新用户（仅管理员）；路径中的 id 会覆盖请求体中的 id。
     *
     * @param id   用户 ID
     * @param user 完整或部分用户实体
     * @return 更新后的用户 VO
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新用户")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserVO> update(@PathVariable String id, @RequestBody User user) {
        user.setId(id);
        User updated = userService.updateUser(user);
        return ApiResponse.ofSuccess(UserMapper.toVO(updated));
    }

    /**
     * 删除用户（仅管理员）。
     *
     * @param id 用户 ID
     * @return 空 data 的成功响应
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        userService.deleteUser(id);
        return ApiResponse.ofSuccess();
    }
}
