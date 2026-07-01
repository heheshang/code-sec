<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  Card, Table, Button, Modal, Form, Select, Tag, Space,
  Typography, message, Popconfirm, Descriptions, Drawer,
} from 'ant-design-vue'
import {
  PlusOutlined, StopOutlined, ReloadOutlined, EyeOutlined,
} from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { http } from '@/api/client'
import { useScanStore } from '@/stores/scan'
import type { RepoListItem } from '@/types/repo'
import type { ScanCreateRequest, ScanTaskResponse } from '@/types/scan'

const scanStore = useScanStore()

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 70 },
  { title: 'Branch', dataIndex: 'branch', key: 'branch', width: 140 },
  { title: 'Mode', dataIndex: 'mode', key: 'mode', width: 80 },
  { title: 'Status', dataIndex: 'status', key: 'status', width: 110 },
  { title: 'Findings', dataIndex: 'findingsCount', key: 'findingsCount', width: 90 },
  { title: 'Started', dataIndex: 'startedAt', key: 'startedAt', width: 170 },
  { title: 'Finished', dataIndex: 'finishedAt', key: 'finishedAt', width: 170 },
  { title: 'Actions', key: 'actions', width: 120 },
]

const projects = ref<RepoListItem[]>([])
const showCreateModal = ref(false)
const createForm = ref<ScanCreateRequest>({ repoId: 0, mode: 'full', branch: 'main' })

// Detail drawer
const showDetail = ref(false)
const detailScan = ref<ScanTaskResponse | null>(null)

const statusColor: Record<string, string> = {
  pending: 'default',
  running: 'processing',
  completed: 'success',
  failed: 'error',
  cancelled: 'warning',
}

async function loadProjects(): Promise<void> {
  try {
    const resp = await http.get('/repos', { params: { page: 1, size: 100 } })
    projects.value = resp.data.items ?? []
  } catch { /* ignore */ }
}

async function openCreate(): Promise<void> {
  await loadProjects()
  createForm.value = { repoId: 0, mode: 'full', branch: 'main' }
  showCreateModal.value = true
}

async function handleCreate(): Promise<void> {
  if (!createForm.value.repoId) {
    message.warning('Please select a repository')
    return
  }
  try {
    await scanStore.create(createForm.value)
    message.success('Scan started')
    showCreateModal.value = false
    await scanStore.fetchList()
  } catch {
    message.error('Failed to start scan')
  }
}

async function handleCancel(id: number): Promise<void> {
  try {
    await scanStore.cancel(id)
    message.success('Scan cancelled')
  } catch {
    message.error('Failed to cancel scan')
  }
}

async function showScanDetail(id: number): Promise<void> {
  try {
    detailScan.value = await scanStore.getById(id)
    showDetail.value = true
  } catch {
    message.error('Failed to load scan details')
  }
}

onMounted(async () => {
  await loadProjects()
  await scanStore.fetchList()
})
</script>

<template>
  <div class="cs-page">
    <PageHeader title="Scans" subtitle="Scan task history and management">
      <template #extra>
        <Space>
          <Select
            v-model:value="(scanStore.repoId as any)"
            placeholder="Filter by repo"
            allow-clear
            style="width: 240px"
            @change="scanStore.fetchList()"
          >
            <Select.Option v-for="p in projects" :key="p.id" :value="p.id">
              {{ p.name }}
            </Select.Option>
          </Select>
          <Button type="primary" @click="openCreate">
            <template #icon><PlusOutlined /></template>
            New Scan
          </Button>
        </Space>
      </template>
    </PageHeader>

    <Card :loading="scanStore.loading">
      <Table
        :data-source="scanStore.items"
        :columns="columns"
        :pagination="{
          total: scanStore.total,
          current: scanStore.page,
          pageSize: scanStore.pageSize,
          showSizeChanger: true,
          showTotal: (t: number) => `Total ${t} items`,
          onChange: (p: number) => { scanStore.setPage(p); scanStore.fetchList() },
        }"
        row-key="id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'mode'">
            <Tag>{{ record.mode }}</Tag>
          </template>
          <template v-if="column.key === 'status'">
            <Tag :color="statusColor[record.status] ?? 'default'">{{ record.status }}</Tag>
          </template>
          <template v-if="column.key === 'startedAt'">
            {{ record.startedAt ? new Date(record.startedAt).toLocaleString() : '-' }}
          </template>
          <template v-if="column.key === 'finishedAt'">
            {{ record.finishedAt ? new Date(record.finishedAt).toLocaleString() : '-' }}
          </template>
          <template v-if="column.key === 'actions'">
            <Space>
              <Button size="small" @click="showScanDetail(record.id)">
                <template #icon><EyeOutlined /></template>
              </Button>
              <Popconfirm
                v-if="record.status === 'pending' || record.status === 'running'"
                title="Cancel this scan?"
                @confirm="handleCancel(record.id)"
              >
                <Button size="small" danger>
                  <template #icon><StopOutlined /></template>
                </Button>
              </Popconfirm>
            </Space>
          </template>
        </template>
      </Table>
    </Card>

    <!-- Create Scan Modal -->
    <Modal
      v-model:open="showCreateModal"
      title="New Scan"
      @ok="handleCreate"
      ok-text="Start Scan"
      :mask-closable="false"
    >
      <Form :model="createForm" layout="vertical">
        <Form.Item label="Repository" :required="true">
          <Select v-model:value="createForm.repoId" placeholder="Select a repository">
            <Select.Option v-for="p in projects" :key="p.id" :value="p.id">
              {{ p.name }}
            </Select.Option>
          </Select>
        </Form.Item>
        <Form.Item label="Mode">
          <Select v-model:value="createForm.mode">
            <Select.Option value="full">Full Scan</Select.Option>
            <Select.Option value="mr">MR Diff Scan</Select.Option>
          </Select>
        </Form.Item>
        <Form.Item label="Branch">
          <Input v-model:value="createForm.branch" placeholder="main" />
        </Form.Item>
        <Form.Item label="Commit SHA (optional)">
          <Input v-model:value="createForm.commitSha" placeholder="Specific commit to scan" />
        </Form.Item>
      </Form>
    </Modal>

    <!-- Scan Detail Drawer -->
    <Drawer
      v-model:open="showDetail"
      title="Scan Details"
      placement="right"
      width="520"
    >
        <Descriptions v-if="detailScan" :column="1" bordered size="small">
        <Descriptions.Item label="ID">{{ detailScan.id }}</Descriptions.Item>
        <Descriptions.Item label="Repo ID">{{ detailScan.repoId }}</Descriptions.Item>
        <Descriptions.Item label="Branch">{{ detailScan.branch }}</Descriptions.Item>
        <Descriptions.Item label="Commit SHA">{{ detailScan.commitSha }}</Descriptions.Item>
        <Descriptions.Item label="Status">
          <Tag :color="statusColor[detailScan.status] ?? 'default'">{{ detailScan.status }}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="Mode">{{ detailScan.mode }}</Descriptions.Item>
        <Descriptions.Item label="Engine">{{ detailScan.engine }}</Descriptions.Item>
        <Descriptions.Item label="Findings">{{ detailScan.findingsCount }}</Descriptions.Item>
        <Descriptions.Item label="Started">{{ detailScan.startedAt ? new Date(detailScan.startedAt).toLocaleString() : '-' }}</Descriptions.Item>
        <Descriptions.Item label="Finished">{{ detailScan.finishedAt ? new Date(detailScan.finishedAt).toLocaleString() : '-' }}</Descriptions.Item>
        <Descriptions.Item v-if="detailScan.errorMessage" label="Error">
          <Typography.Text type="danger">{{ detailScan.errorMessage }}</Typography.Text>
        </Descriptions.Item>
      </Descriptions>
    </Drawer>
  </div>
</template>

<style scoped>
.cs-page {
  padding: var(--cs-space-6);
}
</style>
