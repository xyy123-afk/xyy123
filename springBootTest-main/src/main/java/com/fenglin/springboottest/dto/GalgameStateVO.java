package com.fenglin.springboottest.dto;

import java.util.List;
import java.util.Map;

/**
 * 存档状态快照，读档和存盘都返回它，前端只管覆盖本地状态。
 *
 * @param nodeId      当前停留的节点 id
 * @param history     已读节点 id 数组（原样是 JSON 字符串，前端自己 parse）
 * @param flags       好感度等状态
 * @param ending      已打出且尚未离开的结局 id，可为 null
 * @param playSeconds 累计游玩秒数
 * @param updatedAt   最后保存时间，前端用来显示「上次玩到哪」
 * @param endings     我收集到的全部结局 id
 * @param stats       全站每个结局的达成人数，如 {good: 3, normal: 1}
 */
public record GalgameStateVO(String nodeId,
                             String history,
                             String flags,
                             String ending,
                             Integer playSeconds,
                             String updatedAt,
                             List<String> endings,
                             Map<String, Long> stats) {
}
