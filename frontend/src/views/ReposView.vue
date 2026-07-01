<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  Card, Table, Button, Modal, Form, Input, Select, Space,
  Typography, Tag, message, Popconfirm,
} from 'ant-design-vue'
import {
  PlusOutlined, LinkOutlined, ApiOutlined, DeleteOutlined, EditOutlined, CheckCircleOutlined, CloseCircleOutlined,
} from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { useRepoStore } from '@/stores/repo'
import type { RepoCreateRequest, RepoUpdateRequest, RepoPlatform, RepoStatus } from '@/types/repo'

const repoStore = useRepoStore()

const columns = [
  { title: 'Name', dataIndex: 'name', key: 'name' },
  { title: 'Platform', dataIndex: 'platform', key: 'platform' },
  { title: 'URL', dataIndex: 'url', key: 'url', ellipsis: true },
  { title: 'Business Line', dataIndex: 'businessLine', key: 'businessLine' },
  { title: 'Status', dataIndex: 'status', key: 'status' },
  { title: 'Created', dataIndex: 'createdAt', key: 'createdAt' },
  { title: 'Actions', key: 'actions', width: 200 },
]

const showModal = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<InstanceType<typeof Form> | null>(null)
const form = ref<RepoCreateRequest>({
  name: '',
  platform: 'gitlab',
  url: '',
  accessToken: '',
  webhookSecret: '',
  defaultBranch: 'main',
  businessLine: '',
})

function openCreate(): void {
  editingId.value = null
  form.value = { name: '', platform: 'gitlab', url: '', accessToken: '', webhookSecret: '', defaultBranch: 'main', businessLine: '' }
  showModal.value = true
}

async function openEdit(id: number): Promise<void> {
  const data = await repoStore.getById(id)
  editingId.value = id
  form.value = {
    name: data.name,
    platform: data.platform,
    url: data.url,
    accessToken: '',
    webhookSecret: '',
    defaultBranch: data.defaultBranch,
    businessLine: data.businessLine,
  }
  showModal.value = true
}

async function handleSubmit(): Promise<void> {
  try {
    await (formRef.value as unknown as { validate: () => Promise<void> })?.validate()
    if (editingId.value !== null) {
      const payload: RepoUpdateRequest = {
        name: form.value.name,
        url: form.value.url,
        defaultBranch: form.value.defaultBranch,
        businessLine: form.value.businessLine,
        ...(form.value.accessToken ? { accessToken: form.value.accessToken } : {}),
      }
      await repoStore.update(editingId.value, payload)
      message.success('Repository updated')
    } else {
      await repoStore.create(form.value)
      message.success('Repository created')
    }
    showModal.value = false
  } catch {
    /* validation failed */
  }
}

async function handleDelete(id: number): Promise<void> {
  try {
    await repoStore.remove(id)
    message.success('Repository deleted')
  } catch {
    message.error('Failed to delete repository')
  }
}

async function handleTestConnection(id: number): Promise<void> {
  try {
    const result = await repoStore.testConnection(id)
    if (result.ok) {
      const branchInfo = result.branches?.length ? ` (${result.branches.length} branches)` : ''
      message.success(`Connection OK${branchInfo}`)
    } else {
      message.error(`Connection failed: ${result.error ?? 'Unknown error'}`)
    }
  } catch {
    message.error('Connection test failed')
  }
}

const statusColor: Record<string, string> = {
  active: 'green',
  inactive: 'default',
  error: 'red',
}

onMounted(() => repoStore.fetchList())
</script>

<template>
  <div class="cs-page">
    <PageHeader title="Repositories" subtitle="Manage source code repositories">
      <template #extra>
        <Button type="primary" @click="openCreate">
          <template #icon><PlusOutlined /></template>
          New Repository
        </Button>
      </template>
    </PageHeader>

    <Card :loading="repoStore.loading">
      <Table
        :data-source="repoStore.items"
        :columns="columns"
        :pagination="{
          total: repoStore.total,
          current: repoStore.page,
          pageSize: repoStore.pageSize,
          showSizeChanger: true,
          showTotal: (t: number) => `Total ${t} items`,
          onChange: (p: number) => { repoStore.setPage(p); repoStore.fetchList() },
        }"
        row-key="id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'platform'">
            <Tag>{{ record.platform }}</Tag>
          </template>
          <template v-if="column.key === 'url'">
            <a :href="record.url" target="_blank">
              <LinkOutlined /> {{ record.url }}
            </a>
          </template>
          <template v-if="column.key === 'status'">
            <Tag :color="statusColor[record.status] ?? 'default'">{{ record.status }}</Tag>
          </template>
          <template v-if="column.key === 'createdAt'">
            {{ new Date(record.createdAt).toLocaleDateString() }}
          </template>
          <template v-if="column.key === 'actions'">
            <Space>
              <Button size="small" @click="openEdit(record.id)">
                <template #icon><EditOutlined /></template>
              </Button>
              <Button size="small" @click="handleTestConnection(record.id)">
                <template #icon><ApiOutlined /></template>
              </Button>
              <Popconfirm title="Delete this repository?" @confirm="handleDelete(record.id)">
                <Button size="small" danger>
                  <template #icon><DeleteOutlined /></template>
                </Button>
              </Popconfirm>
            </Space>
          </template>
        </template>
      </Table>
    </Card>

    <Modal
      v-model:open="showModal"
      :title="editingId ? 'Edit Repository' : 'New Repository'"
      @ok="handleSubmit"
      ok-text="Save"
      :mask-closable="false"
    >
      <Form ref="formRef" :model="form" layout="vertical">
        <Form.Item label="Name" name="name" :rules="[{ required: true, message: 'Name is required' }]">
          <Input v-model:value="form.name" placeholder="e.g. my-service" />
        </Form.Item>
        <Form.Item label="URL" name="url" :rules="[{ required: true, message: 'URL is required' }]">
          <Input v-model:value="form.url" placeholder="https://gitlab.com/org/repo" />
        </Form.Item>
        <Form.Item label="Platform" name="platform">
          <Select v-model:value="form.platform">
            <Select.Option value="gitlab">GitLab</Select.Option>
            <Select.Option value="github">GitHub</Select.Option>
            <Select.Option value="gitee">Gitee</Select.Option>
          </Select>
        </Form.Item>
        <Form.Item label="Default Branch" name="defaultBranch">
          <Input v-model:value="form.defaultBranch" placeholder="main" />
        </Form.Item>
        <Form.Item label="Business Line" name="businessLine">
          <Input v-model:value="form.businessLine" placeholder="e.g. Platform Security" />
        </Form.Item>
        <Form.Item label="Access Token" name="accessToken">
          <Input.Password v-model:value="form.accessToken" placeholder="GitLab personal access token" />
        </Form.Item>
        <Form.Item label="Webhook Secret" name="webhookSecret">
          <Input.Password v-model:value="form.webhookSecret" placeholder="Optional webhook token" />
        </Form.Item>
      </Form>
    </Modal>
  </div>
</template>

<style scoped>
.cs-page {
  padding: var(--cs-space-6);
}
</style>
