package com.fenglin.springboottest.dto;

/**
 * 收藏表情包的请求体：url 是 /api/upload 返回的地址，name 是备注（可为空）。
 */
public record StickerAddRequest(String url, String name) {
}
