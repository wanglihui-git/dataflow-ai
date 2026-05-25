package com.dataflow.ai.domain.response;

import lombok.Getter;

/**
 * API 统一响应状态码枚举。
 */
@Getter
public enum ResponseCode {

    // common
    SUCCESS(200, "Success"),
    FAILURE(500, "Fail"),

    // client fail
    CODE_400(400, "Invalid Argument"),
    CODE_401(401, "Not Authorized"),
    CODE_403(403, "Forbidden"),
    CODE_404(404, "Not Found"),
    CODE_409(409, "Conflict"),

    // server fali
    CODE_500(FAILURE.code, FAILURE.getMsg()),
    CODE_501(501, "Server Error"),
    CODE_502(502, "Server Unavailable");

    private final int code;
    private final String msg;

    ResponseCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

}