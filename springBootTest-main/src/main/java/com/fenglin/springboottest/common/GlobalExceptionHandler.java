package com.fenglin.springboottest.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理。
 *
 * <p>没有它的时候，后端一旦抛异常，Spring Boot 会返回一段 HTML 错误页，
 * 前端 api.js 只能看到 "服务器异常（HTTP 500）" 这种看不懂的提示。
 *
 * <p>这里统一包装成前端认得的 { code, msg, data, field } 结构，并且**仍然返回 HTTP 200**：
 * 因为 api.js 遇到非 2xx 会直接丢掉响应体、显示状态码，只有 200 才能把 msg 展示给用户。
 * 业务失败统一用 code=1 表达，和注册/登录的返回风格保持一致。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 请求体不是合法 JSON（比如字段类型不对、JSON 写残缺） */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadJson(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败：{}", e.getMessage());
        return ResponseEntity.ok(ApiResponse.fail("提交的内容格式有误，请重试", ""));
    }

    /** 缺少必填参数，比如 /api/admin/comments 没带 status */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException e) {
        return ResponseEntity.ok(ApiResponse.fail("缺少参数：" + e.getParameterName(), ""));
    }

    /** 唯一索引冲突：slug / 用户名 / 分类名重复都走这里，给一句能看懂的话 */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("唯一键冲突：{}", e.getMessage());
        return ResponseEntity.ok(ApiResponse.fail("这个标识已经被占用了，换一个再试", ""));
    }

    /** 兜底：任何没预料到的异常都记日志，对外只给一句人话 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("服务器内部错误", e);
        return ResponseEntity.ok(ApiResponse.fail("服务器开小差了，请稍后再试", ""));
    }
}
