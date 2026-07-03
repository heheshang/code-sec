<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import { Filter, Download, Refresh, Aim } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/common/PageHeader.vue'
import VulnFilters from '@/components/vuln/VulnFilters.vue'
import VulnTable from '@/components/vuln/VulnTable.vue'
import AiBatchActions from '@/ai-audit/components/AiBatchActions.vue'
import { useVulnStore } from '@/stores/vuln'
import { useAiAuditStore } from '@/ai-audit/stores/aiAuditStore'

const vulnStore = useVulnStore()
const aiAuditStore = useAiAuditStore()
const route = useRoute()

const selectedCount = computed<number>(() => vulnStore.selectedIds.size)

const showBulkDrawer = ref(false)
const showAiBatch = ref(false)

const aiProgressText = computed(() => {
  const total = aiAuditStore.batchTotal
  if (total === 0) return ''
  const done = aiAuditStore.completedCount
  const failed = [...aiAuditStore.progressMap.values()].filter(p => p.status === 'failed').length
  return failed > 0 ? `${done - failed} done / ${failed} failed` : `${done}/${total}`
})

async function runAiBatch() {
  const ids = [...vulnStore.selectedIds]
  if (ids.length === 0) {
    ElMessage.warning('Select at least one vulnerability')
    return
  }
  showAiBatch.value = true
  await aiAuditStore.batchAnalyze(ids)
}

onMounted(async () => {
  const projectId = route.query.project
  if (typeof projectId === 'string' && projectId.length > 0) {
    vulnStore.setFilters({ projectId })
  }
  await vulnStore.fetchList()
})

onUnmounted(() => {
  aiAuditStore.clearResults()
})

let debounceTimer: ReturnType<typeof setTimeout> | null = null

watch(
  () => [
    vulnStore.filters.projectId,
    vulnStore.filters.severity.length,
    vulnStore.filters.status.length,
    vulnStore.filters.exploitability.length,
    vulnStore.filters.keyword,
    vulnStore.page,
    vulnStore.pageSize,
  ],
  () => {
    if (debounceTimer) clearTimeout(debounceTimer)
    debounceTimer = setTimeout(() => {
      void vulnStore.fetchList()
    }, 300)
  },
)
</script>

<template>
  <div class="cs-audit-queue">
    <PageHeader
      title="Audit queue"
      :subtitle="`${vulnStore.total} findings waiting for human review across all projects`"
    >
      <el-tag type="info" effect="plain">vuln store</el-tag>
    </PageHeader>

    <VulnFilters />

    <div class="cs-audit-queue__bar">
      <span class="cs-audit-queue__barLabel">
        <el-icon><Filter /></el-icon> {{ vulnStore.total }} matching
        <span v-if="selectedCount > 0" class="cs-audit-queue__barSelected"> · {{ selectedCount }} selected</span>
      </span>
      <el-space :size="8">
        <el-button :disabled="selectedCount === 0" :loading="aiAuditStore.isAnalyzing" @click="runAiBatch">
          <el-icon><Aim /></el-icon> AI Analyze{{ aiProgressText ? ' (' + aiProgressText + ')' : '' }}
        </el-button>
        <el-button :disabled="selectedCount === 0">
          <el-icon><Refresh /></el-icon> Bulk retest
        </el-button>
        <el-button :disabled="selectedCount === 0">
          <el-icon><Download /></el-icon> Export selected
        </el-button>
      </el-space>
    </div>

    <VulnTable />

    <AiBatchActions v-if="showAiBatch" @close="showAiBatch = false" />

    <el-drawer v-model="showBulkDrawer" title="Bulk action" :size="420" />
  </div>
</template>

<style scoped>
.cs-audit-queue {
  max-width: 1400px;
}
.cs-audit-queue__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--cs-space-2);
  padding: 0 var(--cs-space-1);
}
.cs-audit-queue__barLabel {
  font-size: var(--cs-font-size-sm);
  color: var(--cs-text-secondary);
}
.cs-audit-queue__barSelected {
  color: var(--cs-text-tertiary);
}
</style>
