<script setup lang="ts">
import type { PipelineStageInfo } from '@/types/ai-audit'

defineProps<{
  stages: PipelineStageInfo[]
}>()
</script>

<template>
  <div class="cs-ai-timeline">
    <div
      v-for="(stage, idx) in stages"
      :key="stage.name"
      class="cs-ai-timeline-item"
    >
      <div class="cs-ai-timeline-dot" :class="{ success: stage.success, fail: !stage.success }" />
      <div class="cs-ai-timeline-content">
        <span class="cs-ai-timeline-name">{{ stage.name }}</span>
        <el-tag :type="stage.success ? 'success' : 'danger'" size="small">
          {{ stage.success ? 'Pass' : 'Fail' }}
        </el-tag>
      </div>
      <div v-if="idx < stages.length - 1" class="cs-ai-timeline-line" />
    </div>
  </div>
</template>

<style scoped>
.cs-ai-timeline {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 0;
}

.cs-ai-timeline-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  position: relative;
}

.cs-ai-timeline-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-top: 4px;
  flex-shrink: 0;
}

.cs-ai-timeline-dot.success {
  background: var(--cs-color-accent);
}

.cs-ai-timeline-dot.fail {
  background: var(--cs-severity-critical);
}

.cs-ai-timeline-content {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.cs-ai-timeline-name {
  font-size: 13px;
  color: var(--cs-text-primary);
  text-transform: capitalize;
}

.cs-ai-timeline-line {
  position: absolute;
  left: 4px;
  top: 16px;
  width: 2px;
  height: 12px;
  background: var(--cs-border);
}
</style>
