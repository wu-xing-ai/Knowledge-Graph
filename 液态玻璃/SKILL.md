---
name: liquid-glass
description: 液态玻璃（Liquid Glass）2.0 效果包——Apple 风格玻璃：SVG 位移贴图折射 + Snell 物理表面（凸 Squircle/凹面/唇面）+ 色散 + 弯液面 + 物理高光；含“玻璃即光标”的跟随鼠标透镜（弹簧物理/速度挤压/闲置漂移）。与框架无关，原生 ES Module，仅 Chromium 支持真实折射，其它浏览器自动降级模糊。当用户想要“液态玻璃”“liquid glass”“折射玻璃”“玻璃跟随鼠标”“duangduang 玻璃”“高级感玻璃”效果时使用。
---

# Liquid Glass 2.0 液态玻璃

把 Apple 风格液态玻璃接入任意前端项目。两个能力可独立使用：

| 能力 | API | 说明 |
|---|---|---|
| 鼠标透镜 | `createLiquidMouseLens()` | 玻璃即光标：跟随鼠标、弹簧物理、隐藏原生光标 |
| 任意元素玻璃 | `createLiquidGlass(el, params)` | 把任意元素变成液态玻璃（折射+高光），可拖拽 |

## 文件清单

- `liquidGlass.js` —— 引擎（ES Module，零依赖）：折射 + 物理高光
- `liquidGlass.css` —— 引擎样式（四层：refract / tint / edge / content）
- `liquid-glass-lens.js` —— 跟随鼠标透镜驱动（弹簧物理 + 调好参数）
- `liquid-glass-lens.css` —— 透镜外壳样式 + 光标隐藏 + 水滴质感 rim（亮边/内高光/外投影/厚度着色）
- `demo.html` —— 示例页（需 http 访问，不能 file:// 直开）

## 快速开始：鼠标透镜（推荐）

```html
<link rel="stylesheet" href="liquidGlass.css">
<link rel="stylesheet" href="liquid-glass-lens.css">
<script type="module">
  import { createLiquidMouseLens } from './liquid-glass-lens.js'
  const lens = createLiquidMouseLens()   // 默认 190×120，内置 duangduang 调参
  // 透镜内置调好的 duangduang 参数（见下文「调好的参数」）
  // 改尺寸：createLiquidMouseLens({ width: 200, height: 130, radius: 32 })
  // 改玻璃参数：createLiquidMouseLens({ glass: { scale: 70, dispersion: 0.6 } })
  lens.destroy()                          // 卸载
</script>
```

## 快速开始：任意元素玻璃

宿主元素自己定 宽高 / 定位 / border-radius（引擎按它的 borderRadius 自动对齐折射圆角）：

```html
<link rel="stylesheet" href="liquidGlass.css">
<div id="glass" style="width:260px;height:160px;border-radius:24px;"></div>
<script type="module">
  import { createLiquidGlass } from './liquidGlass.js'
  const g = createLiquidGlass(document.getElementById('glass'), { draggable: true })
  g.update({ scale: 70 })   // 改参数
  g.destroy()               // 卸载
</script>
```

## 调好的参数（duangduang 关键）

鼠标透镜内置以下参数；`createLiquidGlass(el, 参数对象)` 或 `createLiquidMouseLens({ glass: {...} })` 可覆盖：

```json
{
  "mode": "physics",
  "surface": "convexSquircle",
  "specular": "physical",
  "bezel": 30,
  "thickness": 80,
  "scale": 58,
  "specularIntensity": 0.85,
  "blur": 0.5,
  "saturate": 1.2,
  "dispersion": 0.4,
  "lightAngle": 225,
  "bend": 0.4,
  "flip": false
}
```

## 参数速查

| 参数 | 默认 | 说明 |
|---|---|---|
| `mode` | `physics` | `physics`=Snell 物理折射；`fast`=简单余弦剖面（低配） |
| `surface` | `convexSquircle` | `convexCircle` / `convexSquircle` / `concave` / `lip` |
| `specular` | `physical` | `physical`=SVG 高光贴图；`css`=加强 CSS 描边；`none`=关闭 |
| `bezel` | 30 | 折射边缘带宽度 px |
| `thickness` | 80 | 玻璃厚度 px（决定折射强度） |
| `scale` | 58 | feDisplacementMap 基准位移 |
| `specularIntensity` | 0.85 | 高光强度 0~1 |
| `blur` | 0.5 | 折射前高斯模糊 |
| `saturate` | 1.2 | 玻璃内饱和度 |
| `dispersion` | 0.4 | 色散：RGB 三通道位移错位（0=关） |
| `lightAngle` | 225 | 光源角度（度），225=左上 |
| `bend` | 0.4 | 弯液面：玻璃最外缘“裹”一圈背景，液珠感 |
| `flip` | false | 翻转折射方向 |
| `radius` | null | 圆角；null=自动读宿主 border-radius |
| `draggable` | false | 允许拖动元素（纯移动，需宿主已定位） |

`createLiquidGlass` 返回 `{ host, options, update(patch), refresh(), destroy() }`；
`createLiquidMouseLens` 返回 `{ el, glass, update(patch), destroy() }`。

## 实现要点 & 坑

1. **圆角三方一致**：位移贴图 SDF / 高光 / CSS border-radius 由引擎按宿主 borderRadius 自动对齐（`radius` 参数可覆盖）。
2. **不要用 clip-path / mask 裁折射层**：实测与 backdrop-filter 冲突（元素不渲染）。折射圆角只通过 border-radius + 引擎 SDF 对齐实现。
3. **重绘 nudge**：改滤镜属性后需把 backdrop-filter 先设 `none` 再设回 `url()` 强制 Chrome 重绘（引擎已内置）。
4. **背景**：玻璃背后要有非纯色背景（渐变/图片/内容），否则折射看不出来。
5. **平台**：折射核心依赖 SVG 滤镜作 backdrop-filter，仅 Chromium（Chrome/Edge/Electron）支持；其它浏览器自动降级为普通模糊，不报错。
6. **移动端**：`createLiquidMouseLens` 仅在 `(pointer: fine) and (hover: hover)` 的鼠标设备上创建透镜，手机/平板（触屏）自动跳过；`liquid-glass-lens.css` 另带 `@media (pointer: coarse)` 兜底隐藏。给普通元素用的 `createLiquidGlass` 不受影响，移动端可正常用作静态玻璃面板。
7. **性能**：位移贴图仅在尺寸变化时重建；dpr 封顶 2×。低配机可 `specular:'none'`、`mode:'fast'` 或调小透镜。
8. **ES Module**：需通过 http 访问（如 `npx serve .` 或 `python -m http.server`），file:// 直开会被浏览器拦截。

## 验证

用任意静态服务器打开 `demo.html`，移动鼠标应看到：折射玻璃跟随（边缘文字弯折+色散+高光）、可拖动玻璃卡片、静态玻璃面板。控制台无报错即正常。