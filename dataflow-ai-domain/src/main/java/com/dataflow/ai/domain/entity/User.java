package com.dataflow.ai.domain.entity;

import com.dataflow.ai.domain.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    /**
     * 用户ID
     */
    @Id
    private String id;

    /**
     * 用户名
     */
    @Column(unique = true, nullable = false)
    private String username;

    /**
     * 邮笱
     */
    @Column(unique = true)
    private String email;

    /**
     * 密码哈希
     */
    private String passwordHash;

    /**
     * 角色
     */
    @Enumerated(EnumType.STRING)
    private UserRole role;

    /**
     * 部门
     */
    private String department;

    /**
     * 状态
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginAt;
}
