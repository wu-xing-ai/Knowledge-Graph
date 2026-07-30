<template>
  <el-dialog v-model="visible" :title="node?.name || '节点详情'" width="500px">
    <template v-if="node">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="名称">{{ node.name }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          <el-tag>{{ node.type || '知识点' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="所属学科">
          <el-tag :color="node.color" effect="dark" style="color:#fff">{{ node.subject }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="难度">
          <el-rate v-model="node.difficulty" disabled :max="5" />
        </el-descriptions-item>
        <el-descriptions-item label="标签">
          <el-tag v-for="tag in (node.tags || '').split(',')" :key="tag" v-show="tag" size="small" class="tag-item">
            {{ tag }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-if="node.isCross" label="交叉属性">
          <el-tag type="warning">跨学科知识点</el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-if="node.description" label="描述">
          {{ node.description }}
        </el-descriptions-item>
      </el-descriptions>
    </template>
    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button type="primary" @click="$emit('expand', node)">展开关联</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: Boolean,
  node: Object
})
const emit = defineEmits(['update:modelValue', 'expand'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})
</script>

<style scoped>
.tag-item { margin-right: 4px; margin-bottom: 4px; }
</style>
