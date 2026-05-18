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

@ExtendWith(MockitoExtension.class)
class UserRepositoryImplTest {

    @Mock
    private UserJpaRepository jpaRepository;

    @InjectMocks
    private UserRepositoryImpl userRepository;

    @Test
    @DisplayName("findById - 委托 JPA")
    void findById_delegates() {
        when(jpaRepository.findById("u1")).thenReturn(Optional.of(User.builder().id("u1").build()));

        userRepository.findById("u1");

        verify(jpaRepository).findById("u1");
    }

    @Test
    @DisplayName("save - 无 id 时生成 UUID")
    void save_generatesIdWhenMissing() {
        when(jpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User saved = userRepository.save(User.builder().username("a").build());

        assertNotNull(saved.getId());
        verify(jpaRepository).save(any());
    }

    @Test
    @DisplayName("updateLastLoginAt - 委托 JPA")
    void updateLastLoginAt_delegates() {
        userRepository.updateLastLoginAt("u1");

        verify(jpaRepository).updateLastLoginAt(eq("u1"), any());
    }
}
