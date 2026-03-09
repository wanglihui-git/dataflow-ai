package com.dataflow.ai.business.repository.impl;

import com.dataflow.ai.business.repository.UserRepository;
import com.dataflow.ai.domain.entity.User;
import com.dataflow.ai.domain.enums.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户Repository实现（内存存储版本）
 */
@Slf4j
@Repository
public class UserRepositoryImpl implements UserRepository {

    private final Map<String, User> users = new ConcurrentHashMap<>();

    public UserRepositoryImpl() {
        // 初始化默认管理员用户
        User admin = User.builder()
                .id("user_admin")
                .username("admin")
                .email("admin@dataflow.ai")
                .passwordHash("$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH")
                .role(UserRole.ADMIN)
                .department("IT")
                .status("active")
                .createdAt(LocalDateTime.now())
                .build();
        users.put(admin.getId(), admin);
        log.info("Default admin user created: username={}", admin.getUsername());
    }

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return users.values().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return users.values().stream()
                .filter(u -> u.getEmail().equals(email))
                .findFirst();
    }

    @Override
    public List<User> findByRole(UserRole role) {
        return users.values().stream()
                .filter(u -> u.getRole() == role)
                .toList();
    }

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            user.setId(UUID.randomUUID().toString());
        }
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public void deleteById(String id) {
        users.remove(id);
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public void updateLastLoginAt(String userId) {
        User user = users.get(userId);
        if (user != null) {
            user.setLastLoginAt(LocalDateTime.now());
        }
    }
}
