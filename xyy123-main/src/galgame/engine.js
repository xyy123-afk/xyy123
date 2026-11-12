/**
 * Galgame 运行时状态机。
 *
 * 只做一件事：拿剧本（story.js 的 NODES）当数据，算出「现在该显示什么、点下去发生什么」。
 * 所有 UI 都在 GalgameView.vue 里，这里不碰 DOM，方便单测和复用。
 *
 * 关键设计：
 *   - flags 用"增量补丁"合并：节点里的 { favor: 1 } 是"加 1 点"，不是"设成 1"。
 *     这样同一条线里多个分支都能加分，不会互相覆盖。
 *   - 读档只认 nodeId + history + flags 三样，剧本改内容不影响老存档。
 *   - router 节点是纯逻辑节点（type: 'router'），引擎自动跳到 pickEndingBranch 算出的分支，
 *     玩家看不到它，也不会进 history。
 */
import { NODES, ENDINGS, pickEndingBranch } from './story'

export const START_NODE = 'start'

/** 新建一个空状态 */
export function createState() {
  return {
    nodeId: START_NODE,
    /** 已读节点 id，用于"已读"回顾和防止 router 进历史 */
    history: [],
    /** 好感度 / 分支标记 */
    flags: {},
    /** 当前是否停在结局节点 */
    ending: null,
    /** 累计游玩秒数，由页面里的计时器累加后传进来 */
    playSeconds: 0
  }
}

/** 取一个节点；不存在就返回 null，由调用方兜底（存档里的老节点被删时会出现） */
export function getNode(id) {
  return NODES[id] || null
}

/**
 * 合并增量补丁。
 * 数字累加（好感度）、布尔取或（分支标记）、字符串直接覆盖，
 * 其余类型（数组/对象）整体替换——目前剧本没用到，留个明确规则免得以后乱。
 */
export function mergeFlags(flags, patch) {
  if (!patch) return flags
  const next = { ...flags }
  for (const [key, value] of Object.entries(patch)) {
    const prev = next[key]
    if (typeof value === 'number' && typeof prev === 'number') {
      next[key] = prev + value
    } else if (typeof value === 'boolean' && prev === true) {
      next[key] = true
    } else {
      next[key] = value
    }
  }
  return next
}

/**
 * 进入某个节点：合并它的 flags 补丁、写入 history、把 router 一路消化掉。
 * 返回处理完之后的 state 和"最终真正要显示的那个节点"。
 *
 * router 为什么要在进入时就解析掉：如果留到渲染阶段判断，页面会先闪一帧空白，
 * 而且点"继续"时还得再判一次，逻辑会散在两地。
 */
export function enterNode(state, nodeId) {
  let currentId = nodeId
  let flags = state.flags
  const history = [...state.history]
  let guard = 0

  while (guard++ < 32) {
    const node = NODES[currentId]
    if (!node) {
      // 节点不存在：通常是剧本改名后撞上老存档。退回开头，别让玩家卡死在白屏
      return { state: { ...state, nodeId: START_NODE, history: [], flags: {}, ending: null }, node: NODES[START_NODE], broken: true }
    }
    flags = mergeFlags(flags, node.flags)
    if (!node.router && history[history.length - 1] !== currentId) {
      history.push(currentId)
    }
    if (node.router) {
      // 逻辑节点：直接跳到它决定的分支，自身不进 history
      currentId = pickEndingBranch(flags)
      continue
    }
    const nextState = {
      ...state,
      nodeId: currentId,
      history,
      flags,
      ending: node.ending || null
    }
    return { state: nextState, node: node, broken: false }
  }
  // 兜底：router 死循环（自己指向自己）时退回开头
  return { state: { ...state, nodeId: START_NODE, history: [], flags: {}, ending: null }, node: NODES[START_NODE], broken: true }
}

/** 从零开始（新游戏） */
export function startNew() {
  return enterNode(createState(), START_NODE)
}

/** 从存档恢复：nodeId 可能已经不存在，enterNode 会兜底退回开头 */
export function resume(state) {
  return enterNode({ ...createState(), ...state, history: state.history || [], flags: state.flags || {} }, state.nodeId)
}

/**
 * 当前节点"下一步"能去哪。
 * - 有 choices 就是玩家要选
 * - 有 next 字符串就是继续
 * - 都没有（且是结局节点）就是结束
 */
export function nextOf(node) {
  if (!node) return null
  if (Array.isArray(node.choices) && node.choices.length) return { type: 'choice' }
  if (typeof node.next === 'string') return { type: 'continue', next: node.next }
  return { type: 'end' }
}

/**
 * 玩家点了某个选项：把选项自带的 flags 一起合进去，再进入目标节点。
 * 选项里的 flags 和节点的 flags 是两个阶段，都走 mergeFlags，
 * 所以选项加的点数不会被节点里的补丁冲掉。
 */
export function chooseOption(state, option) {
  const withChoiceFlags = { ...state, flags: mergeFlags(state.flags, option.flags) }
  return enterNode(withChoiceFlags, option.next)
}

/** 结局信息（含玩家收集进度里"这个结局是否已达成"） */
export function endingInfo(endingId) {
  return ENDINGS[endingId] || null
}

/** 存档序列化：只留三样必要信息，后端按字符串存 */
export function serialize(state) {
  return {
    nodeId: state.nodeId,
    history: JSON.stringify(state.history || []),
    flags: JSON.stringify(state.flags || {}),
    playSeconds: Math.max(0, Math.floor(state.playSeconds || 0))
  }
}

/** 存档反序列化：任何一段 JSON 坏掉都不应该让游戏崩，退化成空值继续玩 */
export function deserialize(data) {
  if (!data || !data.nodeId) return null
  const parse = (raw, fallback) => {
    try {
      const v = JSON.parse(raw)
      return v === null || v === undefined ? fallback : v
    } catch {
      return fallback
    }
  }
  return {
    nodeId: data.nodeId,
    history: parse(data.history, []),
    flags: parse(data.flags, {}),
    ending: data.ending || null,
    playSeconds: Number(data.playSeconds || 0)
  }
}
