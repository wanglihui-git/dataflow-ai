package com.dataflow.ai.infrastructure.client.llm;

/**
 * LLM 或 Embedding 外部 HTTP API 调用失败时抛出的运行时异常。
 */
public class LlmApiException extends RuntimeException {

    /**
     * @param message 错误描述
     */
    public LlmApiException(String message) {
        super(message);
    }

    /**
     * @param message 错误描述
     * @param cause   原始异常（如 WebClient 响应错误）
     */
    public LlmApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
