/**
 * 《雨夜图书馆》剧本。
 *
 * 设计约束（改剧情时请守住）：
 *   1. 每个节点必须有 id，且全文件唯一；id 一旦发布就不要再改名字——
 *      玩家的存档里存的就是 id，改名等于把所有人的进度弄丢。
 *   2. next 要么是下一个节点的 id 字符串，要么是 [{ text, next }] 选择项数组。
 *   3. flags 是增量补丁：进入节点时把这里的字段合并进全局状态（数字会累加）。
 *      好感度写成 { favor: 1 }，而不是 { favor: 3 }，这样分支多次加也不会算错。
 *   4. ending 字段表示「这里是结局节点」，引擎会停下来显示结局页并上报后端。
 *
 * 场景只有两种画法（见 GalgameStage.vue）：
 *   scene: 'library' —— 图书馆，书架 + 雨窗
 *   scene: 'window'  —— 近景雨窗，情绪戏用
 *   scene: 'home'    —— 家里的书桌
 */

export const GAME_TITLE = '雨夜图书馆'
export const GAME_SUBTITLE = 'The Library in the Rain'

/**
 * 可攻略角色。name 会显示在对话框上方，color 用于名字和立绘高光。
 * 表情（mood）在 GalgameStage 里决定立绘的细微差别，见 MOOD_STYLES。
 */
export const CHARACTERS = {
  lin: { name: '林晚', color: '#8fb8ff' },
  me: { name: '我', color: '#c9d3e3' }
}

/**
 * 结局清单。id 必须和后端 GalgameController 的 VALID_ENDINGS 一致。
 * condition 只是给玩家看的提示文案，实际分支由剧本节点自己控制。
 */
export const ENDINGS = {
  good: {
    id: 'good',
    title: '天亮之前',
    tag: 'TRUE END',
    desc: '你替她记住了那本书，也记住了她。',
    color: '#8fb8ff'
  },
  normal: {
    id: 'normal',
    title: '雨停了',
    tag: 'NORMAL END',
    desc: '你们各自回到自己的时间里，但都留下了点什么。',
    color: '#a8b0bf'
  },
  hidden: {
    id: 'hidden',
    title: '第三排倒数第二格',
    tag: 'HIDDEN END',
    desc: '原来她一直在读的那本书，是你写的。',
    color: '#ffd479'
  }
}

/**
 * 节点表。
 *
 * 开头几张图是「钩子」：一进游戏先把人钉在椅子上，再慢慢铺。
 * 三个结局的触发逻辑：
 *   good    —— 好感度 favor >= 4 且 你替她念了那本书（read: true）
 *   hidden  —— 好感度 favor >= 4，但你先问了她的名字（askedName: true）
 *   normal  —— 其余情况（好感不足，或没做关键选择）
 */
export const NODES = {
  /* ============ 序章：进馆 ============ */

  start: {
    scene: 'library',
    speaker: null,
    text: '十一月，雨下得没完没了。\n我从图书馆的后门溜进来，只想找个能躲雨又有插座的地方，把我那篇写不完的稿子收个尾。',
    next: 'start_2'
  },

  start_2: {
    scene: 'library',
    speaker: null,
    text: '闭馆时间早就过了。灯只留了一半，书架的影子被拉得很长，像一排沉默的人。',
    next: 'met'
  },

  met: {
    scene: 'library',
    speaker: 'lin',
    mood: 'neutral',
    text: '……你也是来躲雨的？',
    next: 'met_2'
  },

  met_2: {
    scene: 'library',
    speaker: null,
    text: '一个女孩坐在靠窗的位子上，面前摊着一本没有书名的书。\n她抬头看我，表情平静得有点不正常——就像这场雨、这栋楼、这个时间点，她都早就知道。',
    next: 'first_choice'
  },

  /* ============ 第一个选择点：靠近方式 ============ */

  first_choice: {
    scene: 'library',
    speaker: 'lin',
    mood: 'neutral',
    text: '她往旁边挪了挪，空出半个椅子。\n要坐吗？',
    choices: [
      { text: '坐下，隔着一点距离', next: 'sit_far', flags: { favor: 1, sit: 'far' } },
      { text: '坐到她旁边', next: 'sit_near', flags: { favor: 2, sit: 'near' } },
      { text: '先站在原地看看她在读什么', next: 'watch_book', flags: { favor: 0, sit: 'stand' } }
    ]
  },

  sit_far: {
    scene: 'library',
    speaker: null,
    text: '我在椅子的另一头坐下，中间隔着一个人的距离。\n她没说什么，只是把那本无名的书往自己那边收了收。',
    next: 'show_book'
  },

  sit_near: {
    scene: 'library',
    speaker: 'lin',
    mood: 'smile',
    text: '我坐过去的时候，袖子擦到了她的手腕。冰凉。\n\n「你不怕冷啊。」我说。\n\n「怕啊。」她笑了一下，「只是习惯了。」',
    next: 'show_book'
  },

  watch_book: {
    scene: 'library',
    speaker: null,
    text: '书页上没有字。\n是一片空白，但又不像新书那样空——更像是什么东西被擦掉了，只留下一层薄薄的白。',
    next: 'show_book'
  },

  show_book: {
    scene: 'window',
    speaker: 'lin',
    mood: 'sad',
    text: '「这本书……我读完了，却什么都记不住。」\n\n她把书推过来一点。\n\n「你说，看过的书如果全忘了，那还算读过吗？」',
    next: 'second_choice'
  },

  /* ============ 第二个选择点：回答 ============ */

  second_choice: {
    scene: 'window',
    speaker: null,
    text: '雨敲在玻璃上，声音很密。\n我该说什么？',
    choices: [
      { text: '「忘掉的也是读过的。」', next: 'answer_soft', flags: { favor: 2 } },
      { text: '「那你记不住自己是谁吗？」', next: 'answer_name', flags: { favor: 1, askedName: true } },
      { text: '「书可能是坏的，我看看。」', next: 'answer_fix', flags: { favor: 1, checked: true } }
    ]
  },

  answer_soft: {
    scene: 'window',
    speaker: 'lin',
    mood: 'smile',
    text: '她愣了一下，然后很轻地笑出声。\n\n「你说话好像书里的人。」\n\n「不好吗？」\n\n「挺好的。」她低下头，「很像我想遇见的那种人。」',
    next: 'bridge'
  },

  answer_name: {
    scene: 'window',
    speaker: null,
    text: '话一出口我就后悔了。\n\n她的手指停在书页边缘，很久没有动。',
    next: 'answer_name_2'
  },

  answer_name_2: {
    scene: 'window',
    speaker: 'lin',
    mood: 'sad',
    text: '「……我叫林晚。」\n\n她说得很慢，像在确认这三个字是不是自己的。\n\n「对，林晚。只是我刚才想了很久才想起来。」',
    next: 'bridge'
  },

  answer_fix: {
    scene: 'library',
    speaker: null,
    text: '我拿起书，翻到中间。书脊有点松，某个位置反复开合过太多次，纸都软了。\n那不是"被擦掉"，是"被读过很多遍"。',
    next: 'answer_fix_2'
  },

  answer_fix_2: {
    scene: 'library',
    speaker: 'lin',
    mood: 'neutral',
    text: '「你怎么知道？」\n\n「书不会骗人。」我说，「翻得最多的地方，一定是最舍不得的那一页。」\n\n她把手按在那一页上，不说话了。',
    next: 'bridge'
  },

  /* ============ 转场：深入 ============ */

  bridge: {
    scene: 'library',
    speaker: null,
    text: '我们有一搭没一搭地聊着。\n她说她常常来这家图书馆，说这里的雨声比任何地方都好听，说她总觉得自己在等一个人。',
    next: 'bridge_2'
  },

  bridge_2: {
    scene: 'window',
    speaker: 'lin',
    mood: 'neutral',
    text: '「你呢？」\n\n「我？我在写一个故事。」我说，「写了很久，写不完。」\n\n「为什么写不完？」',
    next: 'third_choice'
  },

  /* ============ 第三个选择点：故事 ============ */

  third_choice: {
    scene: 'window',
    speaker: null,
    text: '为什么写不完。这个问题我自己都想问自己。',
    choices: [
      { text: '「因为我忘了结局该怎么写。」', next: 'story_end', flags: { favor: 1 } },
      { text: '「因为我不想让它结束。」', next: 'story_never', flags: { favor: 2 } },
      { text: '「要不要你替我写？」', next: 'story_you', flags: { favor: 2, invited: true } }
    ]
  },

  story_end: {
    scene: 'window',
    speaker: 'lin',
    mood: 'smile',
    text: '「那我们一起想。」她说得理所当然。\n\n「你怎么比我还上心。」\n\n「因为我也想知道结局啊。」',
    next: 'rain_stop'
  },

  story_never: {
    scene: 'window',
    speaker: 'lin',
    mood: 'sad',
    text: '她沉默了一会儿。\n\n「可是不结束的东西，就没有人记得住。」\n\n这句话说得很轻，轻到像是说给她自己听的。',
    next: 'rain_stop'
  },

  story_you: {
    scene: 'window',
    speaker: 'lin',
    mood: 'smile',
    text: '她真的接过笔，在书的空白页上写下第一行。\n\n字很漂亮，像刻上去的。\n\n「写好了。」她把书推回来，「到你了。」',
    next: 'rain_stop'
  },

  /* ============ 雨停 ============ */

  rain_stop: {
    scene: 'window',
    speaker: null,
    text: '不知道过了多久。\n雨声小了。窗外路灯的光终于能照进来，落在她的侧脸上。',
    next: 'rain_stop_2'
  },

  rain_stop_2: {
    scene: 'window',
    speaker: 'lin',
    mood: 'sad',
    text: '「雨要停了。」\n\n她说这句话的语气，像在说别的东西要结束了。',
    next: 'final_choice'
  },

  /* ============ 最终选择点：决定结局 ============ */

  final_choice: {
    scene: 'window',
    speaker: null,
    text: '她合上书，站起来。\n我知道她马上就要走了。',
    choices: [
      { text: '「让我把这本书读完吧。」', next: 'end_read', flags: { read: true } },
      { text: '「先告诉我你的名字。」', next: 'end_name', flags: { askedFinal: true } },
      { text: '什么也不说，看着她离开', next: 'end_silent' }
    ]
  },

  end_read: {
    scene: 'window',
    speaker: 'lin',
    mood: 'smile',
    text: '她想了想，把书递给我。\n\n「那你要记住。」\n\n「记住什么？」\n\n「记住你读过。」',
    next: 'end_router'
  },

  end_name: {
    scene: 'window',
    speaker: 'lin',
    mood: 'sad',
    text: '「……林晚。」\n\n她说完，笑了一下，笑得很浅。\n\n「我已经很久没跟人说过这个名字了。」',
    next: 'end_router'
  },

  end_silent: {
    scene: 'window',
    speaker: null,
    text: '我什么也没说。\n\n她走到门口，回头看了我一眼。那一瞬间我很想喊住她，可是我没能想起来该喊什么。',
    next: 'end_router'
  },

  /* ============ 结局分流（不显示文本，纯逻辑节点） ============ */

  end_router: {
    scene: 'library',
    speaker: null,
    text: '',
    router: true
  },

  /* ============ GOOD END ============ */

  good_1: {
    scene: 'library',
    speaker: null,
    text: '我翻开那本书。\n\n第一页是空白的。第二页也是。翻到中间，字才开始出现——是一个我写过的开头。',
    next: 'good_2'
  },

  good_2: {
    scene: 'library',
    speaker: null,
    text: '然后是第二段、第三段。\n是我那篇写不完的稿子。一字不差，连我删掉又写回来的那些句子都在。',
    next: 'good_3'
  },

  good_3: {
    scene: 'library',
    speaker: 'lin',
    mood: 'smile',
    text: '「你写的时候，我就在旁边看。」\n\n她的声音从身后传来。\n\n「你每写完一段，我就读一遍。读了太多遍，就都记住了。」',
    next: 'good_4'
  },

  good_4: {
    scene: 'library',
    speaker: null,
    text: '我转过身。她站在那儿，被透过雨窗的路灯照着，比刚才清楚得多。\n\n「所以这本书里没有字，」我说，「因为字都在你那儿。」',
    next: 'good_5'
  },

  good_5: {
    scene: 'window',
    speaker: 'lin',
    mood: 'smile',
    text: '「那你写结局吧。」她说。\n\n「这次我保证等得到。」',
    ending: 'good',
    next: null
  },

  /* ============ HIDDEN END ============ */

  hidden_1: {
    scene: 'library',
    speaker: null,
    text: '她走后，我在她坐过的位子上坐了很久。\n桌上留了一张纸条，字迹和刚才在书上写的一样。',
    next: 'hidden_2'
  },

  hidden_2: {
    scene: 'library',
    speaker: null,
    text: '「第三排倒数第二格。\n你写不下去的时候，就去看那本书。」',
    next: 'hidden_3'
  },

  hidden_3: {
    scene: 'library',
    speaker: null,
    text: '我找到那个位置。\n那里放着一本旧书，书脊上印着我的名字。',
    next: 'hidden_4'
  },

  hidden_4: {
    scene: 'library',
    speaker: null,
    text: '翻开是空的。\n但扉页上有一行小字，是她的笔迹：\n\n「写下这个名字的人，要先记得自己写过。」',
    next: 'hidden_5'
  },

  hidden_5: {
    scene: 'window',
    speaker: null,
    text: '雨彻底停了。\n我坐在那儿，第一次想起自己为什么来这家图书馆。\n\n不是为了躲雨。\n\n是因为我早就知道，有人在这里等我把故事写完。',
    ending: 'hidden',
    next: null
  },

  /* ============ NORMAL END ============ */

  normal_1: {
    scene: 'window',
    speaker: null,
    text: '雨停了。\n\n她走的时候带走了那本无名的书。桌上只留下一圈淡淡的水痕，是杯子的，也可能不是。',
    next: 'normal_2'
  },

  normal_2: {
    scene: 'home',
    speaker: null,
    text: '我回到家，把稿子打开，发现自己居然能写下去了。\n\n那晚我写了很多，写到一个女孩在雨里的图书馆等一个人。',
    next: 'normal_3'
  },

  normal_3: {
    scene: 'home',
    speaker: null,
    text: '写完最后一个句号，我忽然想不起她的脸。\n\n只记得雨声，和她说"我习惯了"时的那种平静。',
    ending: 'normal',
    next: null
  }
}

/**
 * 分流规则：由引擎在进入 router 节点时调用，返回真正的结局首节点 id。
 * 单独抽成函数而不是塞进 NODES，是因为判断依赖多个 flags 的组合，
 * 写成数据反而看不清优先级——这里的顺序就是优先级，从上往下第一个命中的生效。
 */
export function pickEndingBranch(flags) {
  const favor = Number(flags.favor || 0)
  // 高好感 + 替她读了书 → 真结局
  if (favor >= 4 && flags.read) return 'good_1'
  // 高好感 + 主动问了她是谁 → 隐藏结局（她一直在读你写的东西）
  if (favor >= 4 && (flags.askedName || flags.askedFinal)) return 'hidden_1'
  return 'normal_1'
}
