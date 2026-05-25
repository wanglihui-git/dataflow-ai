package com.dataflow.ai.domain.response;

import java.io.Serializable;

/**
 * 统一 API 响应包装，包含状态码、消息与业务数据。
 *
 * @param <T> 业务数据类型
 */
public class ApiResponse<T> implements Serializable {
    public static final long serialVersionUID = 42L;

    private int code;

    private String msg;

    private T data;

    /** 无参构造，用于反序列化。 */
    public ApiResponse() {}

    /**
     * 构造仅含状态码与消息的响应。
     *
     * @param code 状态码
     * @param msg  提示消息
     */
    public ApiResponse(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    /**
     * 构造完整响应。
     *
     * @param code 状态码
     * @param msg  提示消息
     * @param data 业务数据
     */
    public ApiResponse(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /** @return 状态码 */
    public int getCode() {
        return code;
    }

    /** @param code 状态码 */
    public void setCode(int code) {
        this.code = code;
    }

    /** @return 提示消息 */
    public String getMsg() {
        return msg;
    }

    /** @param msg 提示消息 */
    public void setMsg(String msg) {
        this.msg = msg;
    }

    /** @return 业务数据 */
    public T getData() {
        return data;
    }

    /** @param data 业务数据 */
    public void setData(T data) {
        this.data = data;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Response{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                ", data=" + data +
                '}';
    }


    // --------------------------- tool ---------------------------

    /**
     * 判断当前响应是否成功（状态码为 200）。
     *
     * @return 成功返回 true，否则 false
     */
    public boolean isSuccess() {
        return code == ResponseCode.SUCCESS.getCode();
    }

    /**
     * 判断给定响应是否非空且成功。
     *
     * @param response 待检查的响应
     * @return 成功返回 true，否则 false
     */
    public static boolean isSuccess(ApiResponse<?> response) {
        return response!=null && response.isSuccess();
    }


    // --------------------------- build ---------------------------

    /**
     * 构建带数据的响应。
     *
     * @param code 状态码
     * @param msg  消息
     * @param data 数据
     * @return 响应实例
     */
    public static <T> ApiResponse<T> of(int code, String msg, T data) {
        return new ApiResponse<T>(code, msg, data);
    }

    /**
     * 构建无数据的响应。
     *
     * @param code 状态码
     * @param msg  消息
     * @return 响应实例
     */
    public static <T> ApiResponse<T> of(int code, String msg) {
        return new ApiResponse<T>(code, msg, null);
    }

    /**
     * 构建成功响应（含数据）。
     *
     * @param data 业务数据
     * @return 成功响应
     */
    public static <T> ApiResponse<T> ofSuccess(T data) {
        return new ApiResponse<T>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), data);
    }

    /**
     * 构建成功响应（无数据）。
     *
     * @return 成功响应
     */
    public static <T> ApiResponse<T> ofSuccess() {
        return new ApiResponse<T>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
    }

    /**
     * 构建失败响应（自定义消息）。
     *
     * @param msg 失败原因
     * @return 失败响应
     */
    public static <T> ApiResponse<T> ofFail(String msg) {
        return new ApiResponse<T>(ResponseCode.FAILURE.getCode(), msg, null);
    }

    /**
     * 构建默认失败响应。
     *
     * @return 失败响应
     */
    public static <T> ApiResponse<T> ofFail() {
        return new ApiResponse<T>(ResponseCode.FAILURE.getCode(), ResponseCode.FAILURE.getMsg(), null);
    }


}
