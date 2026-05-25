package com.dataflow.ai.business.repository.impl;

import com.dataflow.ai.business.repository.jpa.PipelineJpaRepository;
import com.dataflow.ai.domain.entity.Pipeline;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

/**
 * PipelineRepositoryImpl 委托与可访问 Pipeline 查询单测。
 */

@ExtendWith(MockitoExtension.class)
class PipelineRepositoryImplTest {

    @Mock
    private PipelineJpaRepository jpaRepository;

    @InjectMocks
    private PipelineRepositoryImpl pipelineRepository;

    /**
     * 验证：findByUser - 合并 owner/public/shared。
     */
    @Test
    @DisplayName("findByUser - 合并 owner/public/shared")
    void findByUser_mergesAccessible() {
        // 准备：配置 Mock 返回值
        org.mockito.Mockito.when(jpaRepository.findByOwnerId("user-001")).thenReturn(java.util.List.of());
        org.mockito.Mockito.when(jpaRepository.findByPermissionLevel(Pipeline.PermissionLevel.PUBLIC))
                .thenReturn(java.util.List.of());
        org.mockito.Mockito.when(jpaRepository.findByPermissionLevel(Pipeline.PermissionLevel.SHARED))
                .thenReturn(java.util.List.of());

        pipelineRepository.findByUser("user-001", "DEVELOPER", "eng");

        // 断言：校验响应或交互
        verify(jpaRepository).findByOwnerId("user-001");
        verify(jpaRepository).findByPermissionLevel(Pipeline.PermissionLevel.PUBLIC);
        verify(jpaRepository).findByPermissionLevel(Pipeline.PermissionLevel.SHARED);
    }

    /**
     * 验证：save - 更新 updatedAt。
     */
    @Test
    @DisplayName("save - 更新 updatedAt")
    void save_setsUpdatedAt() {
        // 准备：配置 Mock 返回值
        org.mockito.Mockito.when(jpaRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(inv -> inv.getArgument(0));

        Pipeline saved = pipelineRepository.save(Pipeline.builder().name("p").build());

        // 断言：校验响应或交互
        org.junit.jupiter.api.Assertions.assertNotNull(saved.getUpdatedAt());
    }

    /**
     * 测试方法 findAccessibleByUserId_integration。
     */
    @Test
    @Disabled("待 Testcontainers + pgvector 环境：验证 findAccessibleByUserId PUBLIC/owner 条件")
    void findAccessibleByUserId_integration() {
    }
}
