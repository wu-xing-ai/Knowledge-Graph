/* =============================================================================
 * Liquid Glass —— 跟随鼠标的折射透镜（duangduang 版）
 * 基于 liquidGlass.js 引擎 + 调好的参数（凸 Squircle 表面 / Snell 物理折射 /
 * 物理高光 / 色散 / 弯液面 bend），叠加弹簧跟随物理：
 *   距离越远跟随越快 · 沿运动主轴挤压 ±15% · 静止 2.5s 后绕鼠标正弦漂移
 * 玻璃即光标：默认隐藏原生光标；仅 pointer:fine 设备启用。
 *
 * 用法：
 *   import { createLiquidMouseLens } from './liquid-glass-lens.js'
 *   const lens = createLiquidMouseLens()                    // 默认 190×120
 *   const lens = createLiquidMouseLens({ width: 200, height: 130, radius: 32,
 *                                        glass: { scale: 70 } })
 *   lens.update({ dispersion: 0.6 })   // 改玻璃参数
 *   lens.destroy()
 * ========================================================================== */

import { createLiquidGlass } from './liquidGlass.js'

const clamp = (v, a, b) => Math.min(b, Math.max(a, v))

/* 交接文档调好的参数：duangduang 的关键 */
const DUAL_GLASS_PARAMS = {
  mode: 'physics',
  surface: 'convexSquircle',
  specular: 'physical',
  bezel: 30,
  thickness: 80,
  scale: 58,
  specularIntensity: 0.85,
  blur: 0.5,
  saturate: 1.2,
  dispersion: 0.4,
  lightAngle: 225,
  bend: 0.4,
  flip: false
}

const LENS_DEFAULTS = {
  width: 190,
  height: 120,
  radius: 30,
  hideCursor: true,
  root: null
}

export function createLiquidMouseLens(options = {}) {
  if (typeof window === 'undefined' || typeof document === 'undefined') return { el: null, destroy() {} }
  // 仅真正的鼠标设备（可悬停）启用；手机/平板等触屏自动跳过（指针为 coarse 或不可悬停）
  if (!window.matchMedia('(pointer: fine) and (hover: hover)').matches) return { el: null, destroy() {} }

  const o = Object.assign({}, LENS_DEFAULTS, options || {})
  const root = o.root || document.body
  const glassParams = Object.assign({}, DUAL_GLASS_PARAMS, options.glass || {})

  const lens = document.createElement('div')
  lens.className = 'lg-mouse-lens'
  lens.style.cssText = [
    'position:fixed', 'left:0', 'top:0',
    'width:' + o.width + 'px',
    'height:' + o.height + 'px',
    'border-radius:' + o.radius + 'px',
    'z-index:9999', 'pointer-events:none', 'opacity:0',
    'will-change:transform'
  ].join(';')
  document.body.appendChild(lens)

  const glass = createLiquidGlass(lens, glassParams)
  if (o.hideCursor) root.classList.add('lg-lens-on')

  /* ---------- 弹簧物理（跟随 + 速度挤压 + 闲置漂移） ---------- */
  let px = window.innerWidth / 2, py = window.innerHeight / 2
  let tx = px, ty = py
  let mx = px, my = py
  let sx = 1, sy = 1
  let lastMove = 0
  let idle = false
  let rafId = 0
  let running = false

  function frame(now) {
    if (!idle && now - lastMove > 2500) idle = true
    if (idle) {
      tx = mx + Math.sin(now * 0.0006) * 36
      ty = my + Math.cos(now * 0.00045) * 28
    }
    const dx = tx - px, dy = ty - py
    const dist = Math.hypot(dx, dy)
    const k = Math.min(0.16 + dist * 0.0015, 0.5) // 距离越远跟随越快
    const nx = px + dx * k, ny = py + dy * k
    const vx = nx - px, vy = ny - py
    px = nx; py = ny

    // 速度挤压：沿运动主轴拉伸、副轴压缩
    const u = clamp(Math.hypot(vx, vy) * 0.012, 0, 0.15)
    let tsx = 1, tsy = 1
    if (u > 0.001) {
      if (Math.abs(vx) >= Math.abs(vy)) { tsx = 1 + u; tsy = 1 - u * 0.7 }
      else { tsy = 1 + u; tsx = 1 - u * 0.7 }
    }
    sx += (tsx - sx) * 0.18
    sy += (tsy - sy) * 0.18

    lens.style.transform =
      'translate(' + (px - o.width / 2).toFixed(1) + 'px,' + (py - o.height / 2).toFixed(1) + 'px)' +
      ' scale(' + sx.toFixed(3) + ',' + sy.toFixed(3) + ')'

    const settled = !idle && dist < 0.3 && Math.abs(tsx - sx) < 0.002 && Math.abs(tsy - sy) < 0.002
    if (settled) { running = false; rafId = 0; return }
    rafId = requestAnimationFrame(frame)
  }

  function tick() { if (!running) { running = true; rafId = requestAnimationFrame(frame) } }

  function onMove(e) {
    mx = e.clientX; my = e.clientY
    tx = mx; ty = my
    lastMove = performance.now()
    idle = false
    lens.style.opacity = '1'
    tick()
  }

  function onLeave() {
    lens.style.opacity = '0'
    running = false
    if (rafId) cancelAnimationFrame(rafId)
    rafId = 0
  }

  window.addEventListener('pointermove', onMove, { passive: true })
  document.documentElement.addEventListener('pointerleave', onLeave)

  return {
    el: lens,
    glass,
    update(patch) { glass.update(patch) },
    destroy() {
      window.removeEventListener('pointermove', onMove)
      document.documentElement.removeEventListener('pointerleave', onLeave)
      if (rafId) cancelAnimationFrame(rafId)
      glass.destroy()
      lens.remove()
      if (o.hideCursor) root.classList.remove('lg-lens-on')
    }
  }
}