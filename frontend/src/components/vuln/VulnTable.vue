<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useVulnStore } from '@/stores/vuln'
import { STATUS_LABEL } from '@/types/vuln'
import type { Vuln, VulnStatus } from '@/types/vuln'
import dayjs from 'dayjs'
import SeverityTagComp from './SeverityTag.vue'
import ExploitabilityBadgeComp from './ExploitabilityBadge.vue'

const router = useRouter()
const vulnStore = useVulnStore()

type StatusTagType = '' | 'primary' | 'success' | 'danger' | 'warning' | 'info'
const statusColorMap: Record<string, StatusTagType> = {
  pending_scan: '',
  pending_audit: '',
  confirmed: 'danger',
  false_positive: '',
  pending_fix: 'primary',
  pending_retest: 'warning',
  fixing: 'primary',
  closed: 'success',
}

function navigateToAudit(id: number): void {
  router.push(`/audit/${id}`)
}

function onSelectionChange(rows: Vuln[]): void {
  vulnStore.clearSelection()
  rows.forEach((r) => vulnStore.toggleSelect(r.id))
}

function onCurrentChange(page: number): void {
  vulnStore.setPage(page)
}

function onSizeChange(size: number): void {
  vulnStore.setPageSize(size)
}
</script>

<template>
  <div class="cs-vuln-table">
    <el-table
      :data="vulnStore.items"
      v-loading="vulnStore.loading"
      :row-key="(r: Vuln) => r.id"
      @selection-change="onSelectionChange"
      @row-click="(r: Vuln) => navigateToAudit(Number(r.id))"
      style="width: 100%"
    >
      <el-table-column type="selection" width="40" />
      <el-table-column label="Severity" width="100">
        <template #default="{ row }">
          <SeverityTagComp v-memo="[row.severity]" :severity="row.severity" size="sm" />
        </template>
      </el-table-column>
      <el-table-column label="Finding" min-width="220">
        <template #default="{ row }">
          <div v-memo="[row.title, row.ruleId, row.cwe, row.cve]">
            <div class="cs-vuln-table__title">{{ row.title }}</div>
            <div class="cs-vuln-table__rule">
              {{ row.ruleId }} · {{ row.cwe }}<template v-if="row.cve !== null"> · {{ row.cve }}</template>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="Project" width="150">
        <template #default="{ row }">
          <span class="cs-vuln-table__project">{{ row.projectId }}</span>
        </template>
      </el-table-column>
      <el-table-column label="File" width="280" show-overflow-tooltip>
        <template #default="{ row }">
          <el-tooltip v-memo="[row.filePath, row.lineStart]" :content="row.filePath" placement="top">
            <span class="cs-vuln-table__file">{{ row.filePath }}:{{ row.lineStart }}</span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="可利用性" width="120">
        <template #default="{ row }">
          <ExploitabilityBadgeComp v-memo="[row.exploitability, row.exploitReason]" :exploitability="row.exploitability" :reason="row.exploitReason" />
        </template>
      </el-table-column>
      <el-table-column label="Status" width="130">
        <template #default="{ row }">
          <span v-memo="[row.status]">
            <el-tag v-if="row.status !== undefined" :type="statusColorMap[row.status] ?? ''" effect="plain">
              {{ STATUS_LABEL[row.status as VulnStatus] ?? row.status }}
            </el-tag>
            <span v-else>—</span>
          </span>
        </template>
      </el-table-column>
      <el-table-column label="Discovered" width="130">
        <template #default="{ row }">
          <span class="cs-vuln-table__date">{{ dayjs(row.discoveredAt).format('MMM D') }}</span>
        </template>
      </el-table-column>
      <template #empty>
        <div style="text-align: center; padding: 24px">
          <strong>No findings match these filters</strong>
          <p style="color: var(--cs-text-tertiary); font-size: var(--cs-font-size-sm); margin: 4px 0 0">
            Try clearing one of the filters above.
          </p>
        </div>
      </template>
    </el-table>
    <div style="display: flex; justify-content: flex-end; padding: 12px 16px; border-top: 1px solid var(--cs-border-light)">
      <el-pagination
        v-model:current-page="vulnStore.page"
        v-model:page-size="vulnStore.pageSize"
        :total="vulnStore.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="onCurrentChange"
        @size-change="onSizeChange"
      />
    </div>
  </div>
</template>

<style scoped>
.cs-vuln-table {
  background: var(--cs-bg-elevated);
  border-radius: var(--cs-radius-md);
  border: 1px solid var(--cs-border-light);
  overflow: hidden;
}
.cs-vuln-table :deep(.el-table__header th) {
  background: var(--cs-bg-sunken);
  font-weight: 600;
  font-size: var(--cs-font-size-xs);
  color: var(--cs-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}
.cs-vuln-table :deep(.el-table__body tr) {
  cursor: pointer;
}
.cs-vuln-table :deep(.el-table__body tr:hover > td) {
  background: var(--cs-bg-hover) !important;
}
.cs-vuln-table :deep(.el-table__body td) {
  font-size: var(--cs-font-size-sm);
}
.cs-vuln-table__title {
  font-weight: 600;
  color: var(--cs-text-primary);
  line-height: 1.4;
}
.cs-vuln-table__rule {
  font-family: var(--cs-font-mono);
  font-size: 11px;
  color: var(--cs-text-tertiary);
  margin-top: 2px;
}
.cs-vuln-table__project {
  font-weight: 500;
  color: var(--cs-text-secondary);
}
.cs-vuln-table__file {
  font-family: var(--cs-font-mono);
  font-size: 12px;
  color: var(--cs-text-secondary);
}
.cs-vuln-table__date {
  font-variant-numeric: tabular-nums;
  color: var(--cs-text-tertiary);
}
</style>
