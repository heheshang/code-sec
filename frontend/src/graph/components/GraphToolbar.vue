<script setup lang="ts">
defineEmits<{
  zoomIn: []
  zoomOut: []
  fitView: []
  exportPng: []
}>()
</script>

<template>
  <div class="cs-graph-toolbar">
    <el-tooltip content="Zoom in" placement="top">
      <el-button size="small" @click="$emit('zoomIn')">
        <el-icon><ZoomIn /></el-icon>
      </el-button>
    </el-tooltip>
    <el-tooltip content="Zoom out" placement="top">
      <el-button size="small" @click="$emit('zoomOut')">
        <el-icon><ZoomOut /></el-icon>
      </el-button>
    </el-tooltip>
    <el-tooltip content="Fit view" placement="top">
      <el-button size="small" @click="$emit('fitView')">
        <el-icon><FullScreen /></el-icon>
      </el-button>
    </el-tooltip>
    <el-tooltip content="Export PNG" placement="top">
      <el-button size="small" @click="$emit('exportPng')">
        <el-icon><Download /></el-icon>
      </el-button>
    </el-tooltip>
  </div>
</template>

<style scoped>
.cs-graph-toolbar {
  display: flex;
  gap: 4px;
  padding: 4px;
  background: var(--cs-bg-elevated);
  border: 1px solid var(--cs-border);
  border-radius: var(--cs-radius-md);
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 10;
}
</style>
