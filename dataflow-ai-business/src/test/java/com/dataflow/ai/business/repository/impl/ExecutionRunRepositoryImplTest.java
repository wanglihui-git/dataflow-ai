package com.dataflow.ai.business.repository.impl;

import com.dataflow.ai.business.repository.jpa.ExecutionRunJpaRepository;
import com.dataflow.ai.domain.entity.ExecutionRun;
import com.dataflow.ai.domain.enums.ExecutionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExecutionRunRepositoryImplTest {

    @Mock
    private ExecutionRunJpaRepository jpaRepository;

    @InjectMocks
    private ExecutionRunRepositoryImpl executionRunRepository;

    @Test
    @DisplayName("countByPipelineId - 委托 JPA")
    void countByPipelineId_delegates() {
        when(jpaRepository.countByPipelineId("pipe-1")).thenReturn(5L);

        long count = executionRunRepository.countByPipelineId("pipe-1");

        assertEquals(5L, count);
    }

    @Test
    @DisplayName("findByPipelineIdAndStatus - 委托 JPA")
    void findByPipelineIdAndStatus_delegates() {
        executionRunRepository.findByPipelineIdAndStatus("pipe-1", ExecutionStatus.RUNNING);

        verify(jpaRepository).findByPipelineIdAndStatus("pipe-1", ExecutionStatus.RUNNING);
    }

    @Test
    @DisplayName("save - 委托 JPA 并生成 id")
    void save_delegates() {
        ExecutionRun run = ExecutionRun.builder().pipelineId("p").status(ExecutionStatus.PENDING).build();
        when(jpaRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

        ExecutionRun saved = executionRunRepository.save(run);

        org.junit.jupiter.api.Assertions.assertNotNull(saved.getId());
        verify(jpaRepository).save(org.mockito.ArgumentMatchers.any());
    }
}
