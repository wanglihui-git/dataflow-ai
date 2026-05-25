package com.dataflow.ai.domain.vo;

import com.dataflow.ai.domain.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户视图（不含密码）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {

    private String id;
    private String username;
    private String email;
    private UserRole role;
    private String department;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
