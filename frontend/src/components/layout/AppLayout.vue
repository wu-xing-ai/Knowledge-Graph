<template>
  <div class="app-layout">
    <Sidebar />
    <div class="main-area">
      <Navbar />
      <div class="content-area">
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onBeforeUnmount } from 'vue'
import Sidebar from './Sidebar.vue'
import Navbar from './Navbar.vue'
import { createLiquidMouseLens } from '@/utils/liquidGlass/liquid-glass-lens'

// 液态玻璃透镜（Skill 版）：glass 即光标，凸 Squircle 折射 + 色散 + 弯液面 + 物理高光
let lens = null
onMounted(() => {
  lens = createLiquidMouseLens({
    width: 160,      // 玻璃宽（px）
    height: 140,     // 玻璃高（px）
    radius: 60,      // 圆角（px），越小倒角越方（70=圆形，20=小圆角方片）
    glass: {
      dispersion: 0.15,        // 色散调淡（原 0.4）
      bend: 0.2,               // 弯液面调淡（原 0.4）
      specularIntensity: 0.5   // 高光减弱（原 0.85）
    }
  })
})
onBeforeUnmount(() => {
  if (lens) lens.destroy()
})
</script>

<style scoped lang="scss">
.app-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  /* 浅色氛围底：为液态玻璃侧栏提供可折射的背景内容 */
  background:
    radial-gradient(900px 620px at -5% -10%, rgba(23, 131, 255, 0.1), transparent 60%),
    radial-gradient(820px 600px at 105% 110%, rgba(13, 148, 136, 0.08), transparent 60%),
    radial-gradient(700px 500px at 60% 115%, rgba(79, 70, 229, 0.06), transparent 60%),
    var(--bg-page);
}
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.content-area {
  flex: 1;
  overflow-y: auto;
  position: relative;
  background: transparent;
}

/* 路由切换动画：新页面从上往下滑入，弹簧缓动与液态玻璃同款 */
.page-enter-from {
  opacity: 0;
  transform: translateY(-16px);
}
.page-enter-active {
  transition:
    opacity 0.25s ease,
    transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.page-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
.page-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}
</style>
