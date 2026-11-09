import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import './utils/liquidGlass/liquidGlass.css'
import './utils/liquidGlass/liquid-glass-lens.css'
import './assets/styles/global.scss'
import { setupLiquidHover } from './utils/liquidHover'

// 液态玻璃悬停交互（高光跟随鼠标 + 卡片微倾斜）
setupLiquidHover()

const app = createApp(App)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { size: 'default' })
app.mount('#app')
