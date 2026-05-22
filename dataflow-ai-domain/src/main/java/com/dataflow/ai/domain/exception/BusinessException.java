package com.dataflow.ai.domain.exception;

import com.dataflow.ai.domain.response.ResponseCode;
import lombok.Getter;

/**
 * 业务异常，携带统一响应码
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    /**
     * 使用指定响应码与自定义消息构造异常。
     *
     * @param responseCode 响应码枚举
     * @param message      错误描述
     */
    public BusinessException(ResponseCode responseCode, String message) {
        super(message);
        this.code = responseCode.getCode();
    }

    /**
     * 使用响应码默认消息构造异常。
     *
     * @param responseCode 响应码枚举
     */
    public BusinessException(ResponseCode responseCode) {
        this(responseCode, responseCode.getMsg());
    }
}
