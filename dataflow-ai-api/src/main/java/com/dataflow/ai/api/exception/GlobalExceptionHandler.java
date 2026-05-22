package com.dataflow.ai.api.exception;

import com.dataflow.ai.domain.exception.BusinessException;
import com.dataflow.ai.domain.response.ApiResponse;
import com.dataflow.ai.domain.response.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局 REST 异常处理器。
 * <p>
 * 将业务异常、参数校验异常与未捕获异常统一转换为 {@link ApiResponse} 格式，
 * 便于前端按 body 中的 code 处理（业务异常 HTTP 状态码仍为 200）。
 * </p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常（如 403、404），HTTP 200，body 携带业务 code。
     *
     * @param ex 业务异常
     * @return 无 data 的 ApiResponse
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleBusiness(BusinessException ex) {
        return ApiResponse.of(ex.getCode(), ex.getMessage());
    }

    /**
     * 处理 {@code @Valid} 校验失败，HTTP 400。
     *
     * @param ex 方法参数校验异常
     * @return 合并后的字段错误信息
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ApiResponse.of(ResponseCode.CODE_400.getCode(), msg);
    }

    /**
     * 处理非法参数，HTTP 400。
     *
     * @param ex 非法参数异常
     * @return 错误消息
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException ex) {
        return ApiResponse.of(ResponseCode.CODE_400.getCode(), ex.getMessage());
    }

    /**
     * 兜底处理未捕获异常，HTTP 500。
     *
     * @param ex 任意异常
     * @return 服务器错误提示
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return ApiResponse.of(ResponseCode.CODE_500.getCode(), ex.getMessage() != null ? ex.getMessage() : "服务器内部错误");
    }
}
