package com.dataflow.ai.business.service;

import com.dataflow.ai.domain.request.LoginRequest;
import com.dataflow.ai.domain.response.LoginResponse;
import com.dataflow.ai.domain.entity.User;
import com.dataflow.ai.domain.enums.UserRole;

import java.util.List;
import java.util.Optional;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 用户登录
     */
    LoginResponse login(LoginRequest request);

    /**
     * 根据ID查询用户
     */
    Optional<User> findById(String id);

    /**
     * 根据用户名查询用户
     */
    Optional<User> findByUsername(String username);

    /**
     * 创建用户
     */
    User createUser(String username, String email, String passwordHash, UserRole role, String department);

    /**
     * 更新用户
     */
    User updateUser(User user);

    /**
     * 删除用户
     */
    void deleteUser(String id);

    /**
     * 查询所有用户
     */
    List<User> findAllUsers();

    /**
     * 更新用户最后登录时间
     */
    void updateLastLogin(String userId);

    /**
     * 修改密码
     */
    void changePassword(String userId, String oldPasswordHash, String newPasswordHash);
}
