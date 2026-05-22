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

    /**
     * 将用户实体转为视图对象（不含密码）。
     *
     * @param user 用户实体，可为 null
     * @return 用户 VO，入参为 null 时返回 null
     */
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

    /**
     * 批量将用户实体列表转为 VO 列表。
     *
     * @param users 用户实体列表
     * @return VO 列表
     */
    public static List<UserVO> toVOList(List<User> users) {
        return users.stream().map(UserMapper::toVO).collect(Collectors.toList());
    }
}
