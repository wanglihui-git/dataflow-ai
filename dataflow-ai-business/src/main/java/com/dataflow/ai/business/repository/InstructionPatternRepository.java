package com.dataflow.ai.business.repository;

import com.dataflow.ai.domain.entity.InstructionPattern;

import java.util.List;
import java.util.Optional;

public interface InstructionPatternRepository {

    Optional<InstructionPattern> findByInstructionHash(String instructionHash);

    List<InstructionPattern> searchByEmbedding(float[] embedding, double minSimilarity, int limit);

    InstructionPattern save(InstructionPattern pattern);
}
