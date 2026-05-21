package com.dataflow.ai.domain.mapper;

import com.dataflow.ai.domain.entity.User;
import com.dataflow.ai.domain.vo.UserVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * User 实体与 VO 映射
 */
public final class UserMapper {

    private UserMapper() {
    }

    public static UserVO toVO(User user) {
        if (user == null) {
            return null;
        }
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .department(user.getDepartment())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    public static List<UserVO> toVOList(List<User> users) {
        return users.stream().map(UserMapper::toVO).collect(Collectors.toList());
    }
}
