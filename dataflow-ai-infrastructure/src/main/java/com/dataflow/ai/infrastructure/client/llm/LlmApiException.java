package com.dataflow.ai.infrastructure.client.llm;

/**
 * LLM / Embedding 外部 API 调用失败
 */
public class LlmApiException extends RuntimeException {

    public LlmApiException(String message) {
        super(message);
    }

    public LlmApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
