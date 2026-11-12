package com.fenglin.springboottest.dto;

/**
 * 保存存档的请求体：POST /api/galgame/save
 *
 * <p>history / flags 从前端原样透传成 JSON 字符串，后端不解析内容，
 * 只做长度和基本格式校验——剧本结构归前端管，后端不加不必要的耦合。
 *
 * @param nodeId      当前停留的节点 id
 * @param history     已读节点 id 数组的 JSON 字符串
 * @param flags       好感度等状态的 JSON 字符串
 * @param playSeconds 本次累计游玩秒数
 * @param ending      记录结局时才有值（good / normal / hidden），普通存盘传 null
 */
public record GalgameSaveRequest(String nodeId, String history, String flags, Integer playSeconds, String ending) {
}
