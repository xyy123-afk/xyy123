/**
 * 全站背景音乐单例。
 *
 * 为什么要单独放到模块里：<audio> 一旦挂在某个组件上，切路由组件销毁音乐就断了。
 * 这里用模块级 Audio 实例，切页面不影响播放，播放状态也持久化到 localStorage。
 *
 * 浏览器限制：没有用户交互（点击/按键）之前不允许自动播放。
 * 所以首次真正播放一定要等用户点一下，播放器组件里靠点击触发。
 */
import { ref } from 'vue'

const VOLUME_KEY = 'tb-bgm-volume'
const PALY_KEY = 'tb-bgm-playing'

/** 想换歌就改这里；文件放 public/music 下，或者直接填外链 */
const TRACKS = ['/music/bgm.mp3']

let audio = null

/** 当前是否在播、当前音量，组件直接绑定 */
export const playing = ref(false)
export const volume = ref(Number(localStorage.getItem(VOLUME_KEY) || 0.4))
export const currentIndex = ref(0)
export const hasError = ref(false)

function ensureAudio() {
  if (audio) return audio
  audio = new Audio()
  audio.loop = true
  audio.volume = volume.value
  audio.preload = 'none'
  audio.src = TRACKS[currentIndex.value]
  audio.addEventListener('play', () => (playing.value = true))
  audio.addEventListener('pause', () => (playing.value = false))
  audio.addEventListener('error', () => {
    hasError.value = true
    playing.value = false
  })
  return audio
}

/** 点播放按钮时调用：这是用户手势，浏览器才允许出声 */
export async function play() {
  const a = ensureAudio()
  try {
    await a.play()
    localStorage.setItem(PALY_KEY, '1')
    hasError.value = false
  } catch {
    hasError.value = true
  }
}

export function pause() {
  if (!audio) return
  audio.pause()
  localStorage.setItem(PALY_KEY, '0')
}

export function toggle() {
  return playing.value ? pause() : play()
}

export function setVolume(v) {
  volume.value = Math.max(0, Math.min(1, v))
  if (audio) audio.volume = volume.value
  localStorage.setItem(VOLUME_KEY, String(volume.value))
}

/** 上次是播放状态的话，组件挂载后等第一次点击再续播（自动播放会被拦） */
export function resumeIfNeeded() {
  return localStorage.getItem(PALY_KEY) === '1'
}
