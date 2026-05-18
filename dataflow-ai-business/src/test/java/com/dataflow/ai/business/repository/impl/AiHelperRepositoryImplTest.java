package com.dataflow.ai.business.repository.impl;

import com.dataflow.ai.business.repository.jpa.AiHelperJpaRepository;
import com.dataflow.ai.domain.entity.AiHelper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiHelperRepositoryImplTest {

    @Mock
    private AiHelperJpaRepository jpaRepository;

    @InjectMocks
    private AiHelperRepositoryImpl aiHelperRepository;

    @Test
    @DisplayName("searchByEmbedding - 转换为 pgvector 字面量并查询")
    void searchByEmbedding_delegatesToNativeQuery() {
        float[] embedding = new float[]{1.0f, 0.0f};
        when(jpaRepository.searchByEmbedding(anyString(), anyDouble(), anyInt())).thenReturn(List.of());

        aiHelperRepository.searchByEmbedding(embedding, 0.8, 5);

        verify(jpaRepository).searchByEmbedding(anyString(), anyDouble(), anyInt());
    }

    @Test
    @DisplayName("save - 生成 id")
    void save_generatesId() {
        when(jpaRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

        AiHelper saved = aiHelperRepository.save(AiHelper.builder().instruction("x").build());

        org.junit.jupiter.api.Assertions.assertNotNull(saved.getId());
    }

    @Test
    @Disabled("待 Testcontainers + pgvector：验证 HNSW 向量检索结果")
    void searchByEmbedding_integration() {
    }
}
