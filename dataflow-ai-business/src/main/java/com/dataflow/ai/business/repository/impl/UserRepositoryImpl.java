package com.dataflow.ai.business.repository.impl;

import com.dataflow.ai.business.repository.UserRepository;
import com.dataflow.ai.business.repository.jpa.UserJpaRepository;
import com.dataflow.ai.domain.entity.User;
import com.dataflow.ai.domain.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 用户Repository实现（PostgreSQL）
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository jpaRepository;

    /**
     * 根据 ID 查询
     */
    @Override
    public Optional<User> findById(String id) {
        return jpaRepository.findById(id);
    }

    /**
     * 根据用户名查询
     */
    @Override
    public Optional<User> findByUsername(String username) {
        return jpaRepository.findByUsername(username);
    }

    /**
     * 根据邮箱查询
     */
    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email);
    }

    /**
     * 根据角色查询用户列表
     */
    @Override
    public List<User> findByRole(UserRole role) {
        return jpaRepository.findByRole(role);
    }

    /**
     * 保存实体
     */
    @Override
    @Transactional
    public User save(User user) {
        if (user.getId() == null) {
            user.setId(UUID.randomUUID().toString());
        }
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(LocalDateTime.now());
        }
        return jpaRepository.save(user);
    }

    /**
     * 根据 ID 删除
     */
    @Override
    @Transactional
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }

    /**
     * 查询全部
     */
    @Override
    public List<User> findAll() {
        return jpaRepository.findAll();
    }

    /**
     * 更新用户最后登录时间
     */
    @Override
    @Transactional
    public void updateLastLoginAt(String userId) {
        jpaRepository.updateLastLoginAt(userId, LocalDateTime.now());
    }
}
