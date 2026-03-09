package com.dataflow.ai.common.dto;

import java.io.Serializable;

public class ApiResponse<T> implements Serializable {
    public static final long serialVersionUID = 42L;

    private int code;

    private String msg;

    private T data;

    public ApiResponse() {}
    public ApiResponse(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
    public ApiResponse(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

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
     * is success
     *
     * @return true if success, false otherwise
     */
    public boolean isSuccess() {
        return code == ResponseCode.SUCCESS.getCode();
    }

    /**
     * is success
     *
     * @param response the response
     * @return true if success, false otherwise
     */
    public static boolean isSuccess(ApiResponse<?> response) {
        return response!=null && response.isSuccess();
    }


    // --------------------------- build ---------------------------

    /**
     * build response
     */
    public static <T> ApiResponse<T> of(int code, String msg, T data) {
        return new ApiResponse<T>(code, msg, data);
    }

    /**
     * build response
     */
    public static <T> ApiResponse<T> of(int code, String msg) {
        return new ApiResponse<T>(code, msg, null);
    }

    /**
     * build success response
     */
    public static <T> ApiResponse<T> ofSuccess(T data) {
        return new ApiResponse<T>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), data);
    }

    /**
     * build success response
     */
    public static <T> ApiResponse<T> ofSuccess() {
        return new ApiResponse<T>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
    }

    /**
     * build fail response
     */
    public static <T> ApiResponse<T> ofFail(String msg) {
        return new ApiResponse<T>(ResponseCode.FAILURE.getCode(), msg, null);
    }

    /**
     * build fail response
     */
    public static <T> ApiResponse<T> ofFail() {
        return new ApiResponse<T>(ResponseCode.FAILURE.getCode(), ResponseCode.FAILURE.getMsg(), null);
    }


}
