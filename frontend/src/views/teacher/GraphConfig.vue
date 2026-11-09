<template>
  <div class="page-container">
    <div class="page-header">
      <h2>交叉学科图谱配置</h2>
      <el-button type="primary" @click="saveConfig"><el-icon><Check /></el-icon>保存配置</el-button>
    </div>

    <el-row :gutter="16">
      <!-- 配置面板 -->
      <el-col :span="8">
        <el-card shadow="never">
          <h4 style="margin-bottom:16px;">图谱样式配置</h4>
          <el-form label-width="80px">
            <el-form-item label="布局模式">
              <el-select v-model="config.layout">
                <el-option label="力导向布局" value="force" />
                <el-option label="环形布局" value="circular" />
                <el-option label="径向布局" value="radial" />
                <el-option label="层次布局" value="dagre" />
              </el-select>
            </el-form-item>
            <el-form-item label="配置名称">
              <el-input v-model="config.name" placeholder="我的图谱配置" />
            </el-form-item>
          </el-form>

          <h4 style="margin:20px 0 12px;">学科配色</h4>
          <div v-for="(color, name) in subjectColors" :key="name" class="color-item">
            <span>{{ name }}</span>
            <div class="color-block" :style="{ background: color }"></div>
          </div>

          <h4 style="margin:20px 0 12px;">已有配置</h4>
          <div v-for="c in savedConfigs" :key="c.id" class="config-item">
            <span>{{ c.name }} ({{ c.layout }})</span>
            <el-button type="danger" text @click="deleteCfg(c.id)">删除</el-button>
          </div>
          <el-empty v-if="!savedConfigs.length" description="暂无保存的配置" :image-size="60" />
        </el-card>
      </el-col>

      <!-- 预览 -->
      <el-col :span="16">
        <el-card shadow="never">
          <h4 style="margin-bottom:12px;">图谱预览</h4>
          <KnowledgeGraph :data="previewData" :layout="config.layout" height="calc(100vh - 280px)" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getGraphData, saveGraphConfig, getGraphConfigs, deleteGraphConfig } from '@/api/graph'
import { SUBJECT_COLORS } from '@/utils/subjectColors'
import KnowledgeGraph from '@/components/KnowledgeGraph.vue'
import { ElMessage } from 'element-plus'

const previewData = ref({ nodes: [], edges: [] })
const savedConfigs = ref([])
const config = reactive({ name: '', layout: 'force' })

// 学科配色统一引用 utils/subjectColors.js（与图谱图例、知识库标签同源）
const subjectColors = SUBJECT_COLORS

async function loadPreview() {
  const res = await getGraphData({ limit: 100 })
  if (res.code === 200) previewData.value = res.data
}

async function loadConfigs() {
  const res = await getGraphConfigs()
  if (res.code === 200) savedConfigs.value = res.data || []
}

async function saveConfig() {
  if (!config.name) {
    ElMessage.warning('请输入配置名称')
    return
  }
  await saveGraphConfig({
    name: config.name,
    layout: config.layout,
    configJson: JSON.stringify(config)
  })
  ElMessage.success('配置已保存')
  loadConfigs()
}

async function deleteCfg(id) {
  await deleteGraphConfig(id)
  ElMessage.success('已删除')
  loadConfigs()
}

onMounted(() => {
  loadPreview()
  loadConfigs()
})
</script>

<style scoped>
.color-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  font-size: 13px;
}
.color-block {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  border: 1px solid #ebeef5;
}
.config-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 13px;
}
</style>
