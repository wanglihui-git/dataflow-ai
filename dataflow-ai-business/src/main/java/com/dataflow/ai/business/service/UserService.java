package com.dataflow.ai.business.service;

import com.dataflow.ai.domain.request.LoginRequest;
import com.dataflow.ai.domain.response.LoginResponse;
import com.dataflow.ai.domain.entity.User;
import com.dataflow.ai.domain.enums.UserRole;

import java.util.List;
import java.util.Optional;

/**
 * 用户账户与认证服务接口。
 * <p>负责登录、令牌刷新、用户 CRUD 及密码变更。</p>
 */
public interface UserService {

    /**
     * 校验用户名密码并签发访问令牌与刷新令牌。
     *
     * @param request 登录请求（用户名、明文密码）
     * @return 含 token、refreshToken 及用户概要信息的响应
     */
    LoginResponse login(LoginRequest request);

    /**
     * 按主键查询用户。
     *
     * @param id 用户 ID
     * @return 用户实体，不存在则为空
     */
    Optional<User> findById(String id);

    /**
     * 按用户名查询用户。
     *
     * @param username 用户名
     * @return 用户实体，不存在则为空
     */
    Optional<User> findByUsername(String username);

    /**
     * 创建新用户（密码应为已哈希值）。
     *
     * @param username     用户名
     * @param email        邮箱
     * @param passwordHash 密码哈希
     * @param role         角色
     * @param department   部门，可为 null
     * @return 持久化后的用户
     */
    User createUser(String username, String email, String passwordHash, UserRole role, String department);

    /**
     * 更新用户信息（全量 save）。
     *
     * @param user 含 ID 的用户实体
     * @return 更新后的用户
     */
    User updateUser(User user);

    /**
     * 按 ID 删除用户。
     *
     * @param id 用户 ID
     */
    void deleteUser(String id);

    /**
     * 查询全部用户列表。
     *
     * @return 用户列表
     */
    List<User> findAllUsers();

    /**
     * 将用户最后登录时间更新为当前时间。
     *
     * @param userId 用户 ID
     */
    void updateLastLogin(String userId);

    /**
     * 校验原密码后更新为新密码（服务端加密存储）。
     *
     * @param userId      用户 ID
     * @param oldPassword 原明文密码
     * @param newPassword 新明文密码
     */
    void changePassword(String userId, String oldPassword, String newPassword);

    /**
     * 使用有效的 refresh token 换取新的 access token。
     *
     * @param refreshToken 刷新令牌
     * @return 新的访问令牌及用户信息
     */
    LoginResponse refreshToken(String refreshToken);
}
