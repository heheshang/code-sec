<script setup lang="ts">
import { computed, h } from 'vue'
import { useRouter } from 'vue-router'
import { Table, Tag, Typography, Space, Tooltip } from 'ant-design-vue'
import type { TableColumnsType, TablePaginationConfig } from 'ant-design-vue'
import { useVulnStore } from '@/stores/vuln'
import { STATUS_LABEL } from '@/types/vuln'
import type { Vuln, VulnStatus, Severity } from '@/types/vuln'
import dayjs from 'dayjs'
import SeverityTagComp from './SeverityTag.vue'
import ExploitabilityBadgeComp from './ExploitabilityBadge.vue'

const router = useRouter()
const vulnStore = useVulnStore()

const projectNameMap = computed<Record<string, string>>(() => {
  // TODO: fetch project names from API when project management endpoint is available
  return {}
})

const statusColorMap: Record<VulnStatus, string> = {
  pending_audit: 'default',
  confirmed: 'error',
  fixing: 'processing',
  pending_retest: 'warning',
  false_positive: 'default',
  closed: 'success',
}

const tableColumns = computed<TableColumnsType<Vuln>>(() => [
  {
    title: 'Severity',
    key: 'severity',
    dataIndex: 'severity',
    width: 100,
    customRender: ({ text }: { text: Severity }) =>
      h(SeverityTagComp, { severity: text, size: 'sm' }),
  },
  {
    title: 'Finding',
    key: 'title',
    dataIndex: 'title',
    ellipsis: true,
    customRender: ({ record }: { record: Vuln }) =>
      h(
        'div',
        {
          style: { cursor: 'pointer' },
          onClick: () => router.push(`/audit/${record.id}`),
        },
        [
          h('div', { class: 'cs-vuln-table__title' }, record.title),
          h(
            'div',
            { class: 'cs-vuln-table__rule' },
            `${record.ruleId} · ${record.cwe}${record.cve !== null ? ` · ${record.cve}` : ''}`,
          ),
        ],
      ),
  },
  {
    title: 'Project',
    key: 'projectId',
    dataIndex: 'projectId',
    width: 150,
    customRender: ({ text }: { text: string }) =>
      h(
        Typography.Text,
        { class: 'cs-vuln-table__project' },
        () => projectNameMap.value[text] ?? text,
      ),
  },
  {
    title: 'File',
    key: 'filePath',
    dataIndex: 'filePath',
    width: 280,
    ellipsis: true,
    customRender: ({ record }: { record: Vuln }) =>
      h(
        Tooltip,
        { title: record.filePath },
        () =>
          h(
            Typography.Text,
            { class: 'cs-vuln-table__file' },
            () => `${record.filePath}:${record.lineStart}`,
          ),
      ),
  },
  {
    title: '可利用性',
    key: 'exploitability',
    dataIndex: 'exploitability',
    width: 120,
    customRender: ({ record }: { record: Vuln }) =>
      h(ExploitabilityBadgeComp, { exploitability: record.exploitability, reason: record.exploitReason }),
  },
  {
    title: 'Status',
    key: 'status',
    dataIndex: 'status',
    width: 130,
    customRender: ({ text }: { text: VulnStatus }) =>
      h(Tag, { color: statusColorMap[text], bordered: false }, () => STATUS_LABEL[text]),
  },
  {
    title: 'Discovered',
    key: 'discoveredAt',
    dataIndex: 'discoveredAt',
    width: 130,
    customRender: ({ text }: { text: string }) =>
      h(Typography.Text, { class: 'cs-vuln-table__date' }, () => dayjs(text).format('MMM D')),
  },
])

const pagination = computed<TablePaginationConfig>(() => ({
  current: vulnStore.page,
  pageSize: vulnStore.pageSize,
  total: vulnStore.total,
  showSizeChanger: true,
  pageSizeOptions: ['10', '20', '50'],
  showTotal: (t: number) => `${t} findings`,
}))

function onTableChange(p: TablePaginationConfig): void {
  if (p.current !== undefined && p.current !== vulnStore.page) vulnStore.setPage(p.current)
  if (p.pageSize !== undefined && p.pageSize !== vulnStore.pageSize) vulnStore.setPageSize(p.pageSize)
}

const rowSelection = computed(() => ({
  selectedRowKeys: Array.from(vulnStore.selectedIds),
  onChange: (keys: (string | number)[]): void => {
    vulnStore.clearSelection()
    keys.forEach((k) => vulnStore.toggleSelect(String(k)))
  },
}))

const emptyText = (): ReturnType<typeof h> =>
  h(Space, { direction: 'vertical', align: 'center', size: 4 }, () => [
    h(Typography.Text, { strong: true }, () => 'No findings match these filters'),
    h(Typography.Text, { type: 'secondary' }, () => 'Try clearing one of the filters above.'),
  ])
</script>

<template>
  <Table
    :columns="tableColumns"
    :data-source="vulnStore.items"
    :loading="vulnStore.loading"
    :pagination="pagination"
    :row-selection="rowSelection"
    :row-key="(r: Vuln) => r.id"
    :scroll="{ x: 1100 }"
    size="middle"
    class="cs-vuln-table"
    @change="onTableChange"
  >
    <template #emptyText>
      <component :is="emptyText" />
    </template>
  </Table>
</template>

<style scoped>
.cs-vuln-table {
  background: var(--cs-bg-elevated);
  border-radius: var(--cs-radius-md);
  border: 1px solid var(--cs-border-light);
  overflow: hidden;
}
.cs-vuln-table :deep(.ant-table-thead > tr > th) {
  background: var(--cs-bg-sunken);
  font-weight: 600;
  font-size: var(--cs-font-size-xs);
  color: var(--cs-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  border-bottom: 1px solid var(--cs-border);
}
.cs-vuln-table :deep(.ant-table-tbody > tr) {
  cursor: pointer;
}
.cs-vuln-table :deep(.ant-table-tbody > tr:hover > td) {
  background: var(--cs-bg-hover) !important;
}
.cs-vuln-table :deep(.ant-table-tbody > tr > td) {
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
