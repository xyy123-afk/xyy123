package com.fenglin.springboottest.common;

/**
 * 统一响应结构，和前端 api.js 的约定保持一致：
 * <pre>
 * 成功：{ code: 0, msg: "ok", data: {...}, field: null }
 * 失败：{ code: 1, msg: "原因", data: null, field: "username" | "password" | "" }
 * </pre>
 * field 告诉前端该高亮哪个输入框，可以为空。
 */
public record ApiResponse<T>(int code, String msg, T data, String field) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "ok", data, null);
    }

    public static <T> ApiResponse<T> fail(String msg, String field) {
        return new ApiResponse<>(1, msg, null, field);
    }
}
