package com.dataflow.ai.business.repository.impl;

import com.dataflow.ai.business.repository.jpa.UserJpaRepository;
import com.dataflow.ai.domain.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UserRepositoryImpl 委托 JPA 单测。
 */

@ExtendWith(MockitoExtension.class)
class UserRepositoryImplTest {

    @Mock
    private UserJpaRepository jpaRepository;

    @InjectMocks
    private UserRepositoryImpl userRepository;

    /**
     * 验证：findById - 委托 JPA。
     */
    @Test
    @DisplayName("findById - 委托 JPA")
    void findById_delegates() {
        // 准备：配置 Mock 返回值
        when(jpaRepository.findById("u1")).thenReturn(Optional.of(User.builder().id("u1").build()));

        userRepository.findById("u1");

        // 断言：校验响应或交互
        verify(jpaRepository).findById("u1");
    }

    /**
     * 验证：save - 无 id 时生成 UUID。
     */
    @Test
    @DisplayName("save - 无 id 时生成 UUID")
    void save_generatesIdWhenMissing() {
        // 准备：配置 Mock 返回值
        when(jpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User saved = userRepository.save(User.builder().username("a").build());

        // 断言：校验响应或交互
        assertNotNull(saved.getId());
        verify(jpaRepository).save(any());
    }

    /**
     * 验证：updateLastLoginAt - 委托 JPA。
     */
    @Test
    @DisplayName("updateLastLoginAt - 委托 JPA")
    void updateLastLoginAt_delegates() {
        userRepository.updateLastLoginAt("u1");

        // 断言：校验响应或交互
        verify(jpaRepository).updateLastLoginAt(eq("u1"), any());
    }
}
