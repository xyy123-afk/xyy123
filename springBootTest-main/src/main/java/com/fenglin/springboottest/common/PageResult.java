package com.fenglin.springboottest.common;

import java.util.List;

/**
 * 分页响应体。
 *
 * <p>待办列表在传了 {@code page} 参数时返回这个结构，否则直接返回数组（兼容旧前端）。
 * 字段含义：
 * <pre>
 *   records 当前页的数据列表
 *   total   总条数
 *   pages   总页数
 *   current 当前第几页（从 1 开始）
 *   size    每页大小
 * </pre>
 */
public record PageResult<T>(List<T> records, long total, long pages, long current, long size) {
}
