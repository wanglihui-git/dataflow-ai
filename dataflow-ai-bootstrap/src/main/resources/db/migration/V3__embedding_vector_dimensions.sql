-- Align pgvector columns with app.embedding.*.dimensions default (1536).
-- Required when columns were created as vector(1024) or unconstrained vector.

DROP INDEX IF EXISTS idx_ai_helpers_embedding;
DROP INDEX IF EXISTS idx_patterns_embedding;

ALTER TABLE ai_helpers
    ALTER COLUMN embedding TYPE vector(1536)
    USING CASE WHEN embedding IS NULL THEN NULL ELSE embedding::vector(1536) END;

ALTER TABLE instruction_patterns
    ALTER COLUMN avg_embedding TYPE vector(1536)
    USING CASE WHEN avg_embedding IS NULL THEN NULL ELSE avg_embedding::vector(1536) END;

CREATE INDEX idx_ai_helpers_embedding ON ai_helpers USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);
CREATE INDEX idx_patterns_embedding ON instruction_patterns USING hnsw (avg_embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);
