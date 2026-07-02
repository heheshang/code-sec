<script setup lang="ts">
import { computed } from 'vue'
import { Search, Refresh, Filter } from '@element-plus/icons-vue'
import { useVulnStore } from '@/stores/vuln'
import type { Severity, VulnStatus, Exploitability } from '@/types/vuln'
import { SEVERITY_LABEL, STATUS_LABEL, EXPLOITABILITY_LABEL } from '@/types/vuln'

const vulnStore = useVulnStore()

const severityOptions = (['critical', 'high', 'medium', 'low', 'info'] as Severity[]).map((s) => ({
  label: SEVERITY_LABEL[s],
  value: s,
}))

const statusOptions = (
  ['pending_scan', 'pending_audit', 'confirmed', 'false_positive', 'pending_fix', 'pending_retest', 'fixing', 'closed'] as VulnStatus[]
).map((s) => ({ label: STATUS_LABEL[s], value: s }))

const exploitOptions = (
  ['exploitable', 'potentially_exploitable', 'not_exploitable'] as Exploitability[]
).map((e) => ({ label: EXPLOITABILITY_LABEL[e], value: e }))

const projectOptions = computed(() => [
  { label: 'All projects', value: '' },
])

const activeCount = computed<number>(() => {
  let n = 0
  if (vulnStore.filters.projectId !== null) n += 1
  if (vulnStore.filters.severity.length > 0) n += 1
  if (vulnStore.filters.status.length > 0) n += 1
  if (vulnStore.filters.exploitability.length > 0) n += 1
  if (vulnStore.filters.keyword.length > 0) n += 1
  return n
})

function onKeywordChange(value: string): void {
  vulnStore.setFilters({ keyword: value })
}

function clearAll(): void {
  vulnStore.resetFilters()
}
</script>

<template>
  <div class="cs-vuln-filters">
    <div class="cs-vuln-filters__row">
      <el-space :size="8" wrap>
        <el-icon class="cs-vuln-filters__icon"><Filter /></el-icon>
        <el-select
          :model-value="vulnStore.filters.projectId ?? ''"
          :options="projectOptions"
          style="width: 180px"
          placeholder="Project"
          @change="(v: string | number) => vulnStore.setFilters({ projectId: v === '' || v === undefined ? null : String(v) })"
        />
        <el-select
          multiple
          :model-value="vulnStore.filters.severity"
          :options="severityOptions"
          style="width: 220px"
          placeholder="Severity"
          collapse-tags
          collapse-tags-tooltip
          clearable
          @change="(v: unknown) => vulnStore.setFilters({ severity: (Array.isArray(v) ? v : []) as Severity[] })"
        />
        <el-select
          multiple
          :model-value="vulnStore.filters.status"
          :options="statusOptions"
          style="width: 260px"
          placeholder="Status"
          collapse-tags
          collapse-tags-tooltip
          clearable
          @change="(v: unknown) => vulnStore.setFilters({ status: (Array.isArray(v) ? v : []) as VulnStatus[] })"
        />
        <el-select
          multiple
          :model-value="vulnStore.filters.exploitability"
          :options="exploitOptions"
          style="width: 220px"
          placeholder="可利用性"
          collapse-tags
          collapse-tags-tooltip
          clearable
          @change="(v: unknown) => vulnStore.setFilters({ exploitability: (Array.isArray(v) ? v : []) as Exploitability[] })"
        />
        <el-input
          :model-value="vulnStore.filters.keyword"
          placeholder="Title, file, CWE…"
          style="width: 260px"
          clearable
          @input="onKeywordChange"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button @click="clearAll" :disabled="activeCount === 0">
          <el-icon><Refresh /></el-icon> Reset
          <span v-if="activeCount > 0" class="cs-vuln-filters__count">{{ activeCount }}</span>
        </el-button>
      </el-space>
    </div>
  </div>
</template>

<style scoped>
.cs-vuln-filters {
  background: var(--cs-bg-elevated);
  border: 1px solid var(--cs-border-light);
  border-radius: var(--cs-radius-md);
  padding: var(--cs-space-3) var(--cs-space-4);
  margin-bottom: var(--cs-space-3);
}
.cs-vuln-filters__icon {
  color: var(--cs-text-tertiary);
  font-size: 14px;
}
.cs-vuln-filters__count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: var(--cs-color-primary);
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  margin-left: 4px;
}
</style>
