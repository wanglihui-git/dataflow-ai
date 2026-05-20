package com.dataflow.ai.domain.exception;

import com.dataflow.ai.domain.response.ResponseCode;
import lombok.Getter;

/**
 * 业务异常，携带统一响应码
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ResponseCode responseCode, String message) {
        super(message);
        this.code = responseCode.getCode();
    }

    public BusinessException(ResponseCode responseCode) {
        this(responseCode, responseCode.getMsg());
    }
}
