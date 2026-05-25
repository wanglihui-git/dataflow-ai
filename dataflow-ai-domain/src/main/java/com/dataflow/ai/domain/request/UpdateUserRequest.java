package com.dataflow.ai.domain.request;

import com.dataflow.ai.domain.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新用户请求体（字段均为可选，仅非 null 字段会写入数据库）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    private String username;

    private String email;

    private UserRole role;

    private String department;

    private String status;
}
