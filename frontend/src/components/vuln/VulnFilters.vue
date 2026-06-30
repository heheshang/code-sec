<script setup lang="ts">
import { computed } from 'vue'
import { Select, Input, Button, Space } from 'ant-design-vue'
import { ReloadOutlined, FilterOutlined } from '@ant-design/icons-vue'
import { useVulnStore } from '@/stores/vuln'
import type { Severity, VulnStatus, Exploitability } from '@/types/vuln'
import { SEVERITY_LABEL, STATUS_LABEL, EXPLOITABILITY_LABEL } from '@/types/vuln'
import { projects as projectList } from '@/api/mock/data'

const vulnStore = useVulnStore()

const severityOptions = (['critical', 'high', 'medium', 'low', 'info'] as Severity[]).map((s) => ({
  label: SEVERITY_LABEL[s],
  value: s,
}))

const statusOptions = (
  ['pending_audit', 'confirmed', 'fixing', 'pending_retest', 'false_positive', 'closed'] as VulnStatus[]
).map((s) => ({ label: STATUS_LABEL[s], value: s }))

const exploitOptions = (
  ['EXPLOITABLE', 'POTENTIALLY_EXPLOITABLE', 'NOT_EXPLOITABLE'] as Exploitability[]
).map((e) => ({ label: EXPLOITABILITY_LABEL[e], value: e }))

const projectOptions = computed(() => [
  { label: 'All projects', value: '' },
  ...projectList.map((p) => ({ label: p.name, value: p.id })),
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

function onKeywordChange(e: Event): void {
  const value = (e.target as HTMLInputElement).value
  vulnStore.setFilters({ keyword: value })
}

function clearAll(): void {
  vulnStore.resetFilters()
}
</script>

<template>
  <div class="cs-vuln-filters">
    <div class="cs-vuln-filters__row">
      <Space :size="8" wrap>
        <FilterOutlined class="cs-vuln-filters__icon" />
        <Select
          :value="vulnStore.filters.projectId ?? ''"
          :options="projectOptions"
          style="width: 180px"
          placeholder="Project"
          @change="(v: unknown) => vulnStore.setFilters({ projectId: v === '' || v === undefined ? null : String(v) })"
        />
        <Select
          mode="multiple"
          :value="vulnStore.filters.severity"
          :options="severityOptions"
          style="width: 220px"
          placeholder="Severity"
          :max-tag-count="2"
          allow-clear
          @change="(v: unknown) => vulnStore.setFilters({ severity: (Array.isArray(v) ? v : []) as Severity[] })"
        />
        <Select
          mode="multiple"
          :value="vulnStore.filters.status"
          :options="statusOptions"
          style="width: 260px"
          placeholder="Status"
          :max-tag-count="2"
          allow-clear
          @change="(v: unknown) => vulnStore.setFilters({ status: (Array.isArray(v) ? v : []) as VulnStatus[] })"
        />
        <Select
          mode="multiple"
          :value="vulnStore.filters.exploitability"
          :options="exploitOptions"
          style="width: 220px"
          placeholder="可利用性"
          :max-tag-count="2"
          allow-clear
          @change="(v: unknown) => vulnStore.setFilters({ exploitability: (Array.isArray(v) ? v : []) as Exploitability[] })"
        />
        <Input.Search
          :value="vulnStore.filters.keyword"
          placeholder="Title, file, CWE…"
          style="width: 260px"
          allow-clear
          @change="onKeywordChange"
        />
        <Button @click="clearAll" :disabled="activeCount === 0">
          <ReloadOutlined /> Reset
          <span v-if="activeCount > 0" class="cs-vuln-filters__count">{{ activeCount }}</span>
        </Button>
      </Space>
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
