<script setup lang="ts">
import { computed } from 'vue'
import { Close } from '@element-plus/icons-vue'
import { useAiAuditStore } from '../stores/aiAuditStore'

const emit = defineEmits<{
  close: []
}>()

const store = useAiAuditStore()

const progressText = computed(() => {
  const total = store.batchTotal
  if (total === 0) return 'No results'
  const done = store.completedCount
  const failed = [...store.progressMap.values()].filter(p => p.status === 'failed').length
  return failed > 0 ? `${done - failed} done / ${failed} failed` : `${done}/${total} complete`
})

function handleClose() {
  emit('close')
}
</script>

<template>
  <div v-if="store.isAnalyzing || store.completedCount > 0" class="cs-ai-batch-progress">
    <el-card shadow="never">
      <div class="cs-ai-batch-header">
        <span class="cs-ai-batch-title">Batch AI Analysis</span>
        <el-space :size="4">
          <span class="cs-ai-batch-status">{{ progressText }}</span>
          <el-button v-if="!store.isAnalyzing" size="small" text @click="handleClose">
            <el-icon><Close /></el-icon>
          </el-button>
        </el-space>
      </div>
      <el-progress
        v-if="store.isAnalyzing"
        :percentage="store.batchTotal > 0 ? Math.round((store.completedCount / store.batchTotal) * 100) : 0"
        :stroke-width="6"
      />
    </el-card>
  </div>
</template>

<style scoped>
.cs-ai-batch-progress {
  margin-top: var(--cs-space-3);
}
.cs-ai-batch-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.cs-ai-batch-title {
  font-weight: 600;
  font-size: var(--cs-font-size-sm);
}
.cs-ai-batch-status {
  font-size: var(--cs-font-size-xs);
  color: var(--cs-text-tertiary);
}
</style>
