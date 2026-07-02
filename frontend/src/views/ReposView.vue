<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Plus, Link, Connection, Delete, Edit } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/common/PageHeader.vue'
import { useRepoStore } from '@/stores/repo'
import type { RepoCreateRequest, RepoUpdateRequest } from '@/types/repo'

const repoStore = useRepoStore()

const showModal = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref()
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
    await formRef.value?.validate()
    if (editingId.value !== null) {
      const payload: RepoUpdateRequest = {
        name: form.value.name,
        url: form.value.url,
        defaultBranch: form.value.defaultBranch,
        businessLine: form.value.businessLine,
        ...(form.value.accessToken ? { accessToken: form.value.accessToken } : {}),
      }
      await repoStore.update(editingId.value, payload)
      ElMessage.success('Repository updated')
    } else {
      await repoStore.create(form.value)
      ElMessage.success('Repository created')
    }
    showModal.value = false
  } catch {
    /* validation failed */
  }
}

async function handleDelete(id: number): Promise<void> {
  try {
    await repoStore.remove(id)
    ElMessage.success('Repository deleted')
  } catch {
    ElMessage.error('Failed to delete repository')
  }
}

async function handleTestConnection(id: number): Promise<void> {
  try {
    const result = await repoStore.testConnection(id)
    if (result.ok) {
      const branchInfo = result.branches?.length ? ` (${result.branches.length} branches)` : ''
      ElMessage.success(`Connection OK${branchInfo}`)
    } else {
      ElMessage.error(`Connection failed: ${result.error ?? 'Unknown error'}`)
    }
  } catch {
    ElMessage.error('Connection test failed')
  }
}

type StatusType = '' | 'primary' | 'success' | 'danger' | 'warning' | 'info'
const statusType: Record<string, StatusType> = {
  active: 'success',
  inactive: '',
  error: 'danger',
}

onMounted(() => repoStore.fetchList())
</script>

<template>
  <div class="cs-page">
    <PageHeader title="Repositories" subtitle="Manage source code repositories">
      <template #extra>
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon> New Repository
        </el-button>
      </template>
    </PageHeader>

    <el-card v-loading="repoStore.loading">
      <el-table
        :data="repoStore.items"
        row-key="id"
      >
        <el-table-column label="Name" prop="name" />
        <el-table-column label="Platform" prop="platform">
          <template #default="{ row }">
            <el-tag>{{ row.platform }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="URL" prop="url" show-overflow-tooltip>
          <template #default="{ row }">
            <a :href="row.url" target="_blank">
              <el-icon><Link /></el-icon> {{ row.url }}
            </a>
          </template>
        </el-table-column>
        <el-table-column label="Business Line" prop="businessLine" />
        <el-table-column label="Status" prop="status">
          <template #default="{ row }">
            <el-tag :type="statusType[row.status] ?? ''">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Created" prop="createdAt">
          <template #default="{ row }">
            {{ new Date(row.createdAt).toLocaleDateString() }}
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="200">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row.id)">
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-button size="small" @click="handleTestConnection(row.id)">
              <el-icon><Connection /></el-icon>
            </el-button>
            <el-popconfirm title="Delete this repository?" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button size="small" type="danger">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div style="display: flex; justify-content: flex-end; margin-top: 12px">
        <el-pagination
          v-model:current-page="repoStore.page"
          v-model:page-size="repoStore.pageSize"
          :total="repoStore.total"
          layout="total, sizes, prev, pager, next"
          @current-change="() => repoStore.fetchList()"
          @size-change="() => repoStore.fetchList()"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="showModal"
      :title="editingId ? 'Edit Repository' : 'New Repository'"
      @confirm="handleSubmit"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" label-position="top">
        <el-form-item label="Name" prop="name" :rules="[{ required: true, message: 'Name is required' }]">
          <el-input v-model="form.name" placeholder="e.g. my-service" />
        </el-form-item>
        <el-form-item label="URL" prop="url" :rules="[{ required: true, message: 'URL is required' }]">
          <el-input v-model="form.url" placeholder="https://gitlab.com/org/repo" />
        </el-form-item>
        <el-form-item label="Platform" prop="platform">
          <el-select v-model="form.platform">
            <el-option value="gitlab" label="GitLab" />
            <el-option value="github" label="GitHub" />
            <el-option value="gitee" label="Gitee" />
          </el-select>
        </el-form-item>
        <el-form-item label="Default Branch" prop="defaultBranch">
          <el-input v-model="form.defaultBranch" placeholder="main" />
        </el-form-item>
        <el-form-item label="Business Line" prop="businessLine">
          <el-input v-model="form.businessLine" placeholder="e.g. Platform Security" />
        </el-form-item>
        <el-form-item label="Access Token" prop="accessToken">
          <el-input v-model="form.accessToken" type="password" placeholder="GitLab personal access token" show-password />
        </el-form-item>
        <el-form-item label="Webhook Secret" prop="webhookSecret">
          <el-input v-model="form.webhookSecret" type="password" placeholder="Optional webhook token" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showModal = false">Cancel</el-button>
        <el-button type="primary" @click="handleSubmit">Save</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
</style>
