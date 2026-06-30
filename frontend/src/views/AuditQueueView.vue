<script setup lang="ts">
import { onMounted, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import { Button, Space, Tag, Typography, Drawer } from 'ant-design-vue'
import { FilterOutlined, ExportOutlined, ThunderboltOutlined } from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import VulnFilters from '@/components/vuln/VulnFilters.vue'
import VulnTable from '@/components/vuln/VulnTable.vue'
import { useVulnStore } from '@/stores/vuln'

const vulnStore = useVulnStore()
const route = useRoute()

const selectedCount = computed<number>(() => vulnStore.selectedIds.size)

const showBulkDrawer = computed({
  get: () => false,
  set: () => undefined,
})

onMounted(async () => {
  const projectId = route.query.project
  if (typeof projectId === 'string' && projectId.length > 0) {
    vulnStore.setFilters({ projectId })
  }
  await vulnStore.fetchList()
})

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
    void vulnStore.fetchList()
  },
)
</script>

<template>
  <div class="cs-audit-queue">
    <PageHeader
      title="Audit queue"
      :subtitle="`${vulnStore.total} findings waiting for human review across all projects`"
    >
      <Tag color="purple" bordered>vuln store</Tag>
    </PageHeader>

    <VulnFilters />

    <div class="cs-audit-queue__bar">
      <Space :size="8">
        <Typography.Text class="cs-audit-queue__barLabel">
          <FilterOutlined /> {{ vulnStore.total }} matching
        </Typography.Text>
        <Typography.Text v-if="selectedCount > 0" type="secondary">
          · {{ selectedCount }} selected
        </Typography.Text>
      </Space>
      <Space :size="8">
        <Button :disabled="selectedCount === 0">
          <ThunderboltOutlined /> Bulk retest
        </Button>
        <Button :disabled="selectedCount === 0">
          <ExportOutlined /> Export selected
        </Button>
      </Space>
    </div>

    <VulnTable />

    <Drawer v-model:open="showBulkDrawer" title="Bulk action" :width="420" />
  </div>
</template>

<style scoped>
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
</style>
