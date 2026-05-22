package com.dataflow.ai.api.exception;

import com.dataflow.ai.domain.exception.BusinessException;
import com.dataflow.ai.domain.response.ResponseCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * GlobalExceptionHandler 各类异常到 ApiResponse 的映射测试。
 */

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    /**
     * 验证：BusinessException 映射为 ApiResponse。
     */
    @Test
    @DisplayName("BusinessException 映射为 ApiResponse")
    void businessException() {
        var response = handler.handleBusiness(
                new BusinessException(ResponseCode.CODE_404, "未找到"));
        // 断言：校验响应或交互
        assertEquals(404, response.getCode());
        assertEquals("未找到", response.getMsg());
    }
}
