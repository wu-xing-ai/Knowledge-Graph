// 液态玻璃悬停交互（参考 Liquid Glass Studio 的指针物理）
// 命中元素在 pointermove 时更新 --mx/--my（高光跟随鼠标位置），
// 卡片类元素额外更新 --rx/--ry（小角度 3D 倾斜），离开时由 CSS 弹簧缓动复位。
// 事件全部委托到 document，无需改动任何组件模板。

const SHEEN_SELECTOR =
  '.stat-card, .topic-card, .glass-card, .welcome, .side-menu .el-menu-item, .el-button--primary'
const TILT_SELECTOR = '.stat-card, .topic-card'
const TILT_MAX_DEG = 4

export function setupLiquidHover() {
  if (typeof window === 'undefined' || typeof document === 'undefined') return
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return

  let current = null
  let rafId = 0

  function update(e) {
    const el = current
    if (!el) return
    const rect = el.getBoundingClientRect()
    if (rect.width === 0 || rect.height === 0) return
    const px = ((e.clientX - rect.left) / rect.width) * 100
    const py = ((e.clientY - rect.top) / rect.height) * 100
    el.style.setProperty('--mx', `${px.toFixed(2)}%`)
    el.style.setProperty('--my', `${py.toFixed(2)}%`)

    if (el.matches(TILT_SELECTOR)) {
      // 光标位置映射为 ±TILT_MAX_DEG 的倾斜角
      const rx = (0.5 - py / 100) * 2 * TILT_MAX_DEG
      const ry = (px / 100 - 0.5) * 2 * TILT_MAX_DEG
      el.style.setProperty('--rx', `${rx.toFixed(2)}deg`)
      el.style.setProperty('--ry', `${ry.toFixed(2)}deg`)
    }
  }

  function reset() {
    if (!current) return
    current.classList.remove('lg-hovering')
    current.style.removeProperty('--rx')
    current.style.removeProperty('--ry')
    current = null
  }

  document.addEventListener('pointerover', (e) => {
    const el = e.target.closest?.(SHEEN_SELECTOR) || null
    if (el !== current) {
      reset()
      current = el
      if (current) current.classList.add('lg-hovering')
    }
  })

  document.addEventListener('pointermove', (e) => {
    if (!current || rafId) return
    rafId = requestAnimationFrame(() => {
      rafId = 0
      update(e)
    })
  })

  document.addEventListener('pointerout', (e) => {
    if (current && !current.contains(e.relatedTarget)) reset()
  })

  document.addEventListener('pointerleave', reset)
}
