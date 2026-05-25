package com.dataflow.ai.business.repository;

import com.dataflow.ai.domain.entity.InstructionPattern;

import java.util.List;
import java.util.Optional;

/**
 * 历史指令模式仓储接口
 */
public interface InstructionPatternRepository {

    /** 根据指令哈希查询 */
    Optional<InstructionPattern> findByInstructionHash(String instructionHash);

    /** 基于向量嵌入的相似度搜索 */
    List<InstructionPattern> searchByEmbedding(float[] embedding, double minSimilarity, int limit);

    /** 保存指令模式 */
    InstructionPattern save(InstructionPattern pattern);
}
