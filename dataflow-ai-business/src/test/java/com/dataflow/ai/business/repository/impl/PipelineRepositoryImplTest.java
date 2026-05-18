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

@ExtendWith(MockitoExtension.class)
class PipelineRepositoryImplTest {

    @Mock
    private PipelineJpaRepository jpaRepository;

    @InjectMocks
    private PipelineRepositoryImpl pipelineRepository;

    @Test
    @DisplayName("findByUser - 委托原生 SQL 查询")
    void findByUser_delegates() {
        pipelineRepository.findByUser("user-001");

        verify(jpaRepository).findAccessibleByUserId("user-001");
    }

    @Test
    @DisplayName("save - 更新 updatedAt")
    void save_setsUpdatedAt() {
        org.mockito.Mockito.when(jpaRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(inv -> inv.getArgument(0));

        Pipeline saved = pipelineRepository.save(Pipeline.builder().name("p").build());

        org.junit.jupiter.api.Assertions.assertNotNull(saved.getUpdatedAt());
    }

    @Test
    @Disabled("待 Testcontainers + pgvector 环境：验证 findAccessibleByUserId PUBLIC/owner 条件")
    void findAccessibleByUserId_integration() {
    }
}
