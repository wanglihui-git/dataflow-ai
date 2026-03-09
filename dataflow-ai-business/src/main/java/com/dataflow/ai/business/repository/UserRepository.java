package com.dataflow.ai.business.repository;

import com.dataflow.ai.domain.entity.User;
import com.dataflow.ai.domain.enums.UserRole;

import java.util.List;
import java.util.Optional;

/**
 * 用户Repository接口
 */
public interface UserRepository {

    /**
     * 根据ID查询用户
     */
    Optional<User> findById(String id);

    /**
     * 根据用户名查询用户
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据邮箱查询用户
     */
    Optional<User> findByEmail(String email);

    /**
     * 根据角色查询用户列表
     */
    List<User> findByRole(UserRole role);

    /**
     * 保存用户
     */
    User save(User user);

    /**
     * 删除用户
     */
    void deleteById(String id);

    /**
     * 查询所有用户
     */
    List<User> findAll();

    /**
     * 更新用户最后登录时间
     */
    void updateLastLoginAt(String userId);
}
