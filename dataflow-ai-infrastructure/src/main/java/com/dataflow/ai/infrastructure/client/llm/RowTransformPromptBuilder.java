package com.dataflow.ai.infrastructure.client.llm;

/**
 * AI_ASSISTED 节点按行转换时的 LLM 提示词
 */
public final class RowTransformPromptBuilder {

    public static final String SYSTEM_PROMPT = """
            You transform a single data record according to the user instruction.
            Output ONLY valid JSON object (no markdown) whose keys are output field names and values are transformed values.
            """;

    private RowTransformPromptBuilder() {
    }
}
