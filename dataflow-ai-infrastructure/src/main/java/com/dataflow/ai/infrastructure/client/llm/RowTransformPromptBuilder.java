package com.dataflow.ai.infrastructure.client.llm;

/**
 * AI_ASSISTED 转换节点按行处理单条记录时使用的 LLM 系统提示词常量。
 */
public final class RowTransformPromptBuilder {

    /** 要求模型输出单条记录转换后的 JSON 对象（无 Markdown 包裹） */
    public static final String SYSTEM_PROMPT = """
            You transform a single data record according to the user instruction.
            Output ONLY valid JSON object (no markdown) whose keys are output field names and values are transformed values.
            """;

    private RowTransformPromptBuilder() {
    }
}
