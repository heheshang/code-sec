<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Plus, CircleClose, Refresh, View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/common/PageHeader.vue'
import { http } from '@/api/client'
import { useScanStore } from '@/stores/scan'
import type { RepoListItem } from '@/types/repo'
import type { ScanCreateRequest, ScanTaskResponse } from '@/types/scan'

const scanStore = useScanStore()

const projects = ref<RepoListItem[]>([])
const showCreateModal = ref(false)
const createForm = ref<ScanCreateRequest>({ repoId: 0, mode: 'full', branch: 'main' })

// Detail drawer
const showDetail = ref(false)
const detailScan = ref<ScanTaskResponse | null>(null)

type StatusType = '' | 'primary' | 'success' | 'danger' | 'warning' | 'info'
const statusType: Record<string, StatusType> = {
  pending: '',
  running: 'primary',
  completed: 'success',
  failed: 'danger',
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
    ElMessage.warning('Please select a repository')
    return
  }
  try {
    await scanStore.create(createForm.value)
    ElMessage.success('Scan started')
    showCreateModal.value = false
    scanStore.setRepoId(createForm.value.repoId)
    await scanStore.fetchList()
  } catch {
    ElMessage.error('Failed to start scan')
  }
}

async function handleCancel(id: number): Promise<void> {
  try {
    await scanStore.cancel(id)
    ElMessage.success('Scan cancelled')
  } catch {
    ElMessage.error('Failed to cancel scan')
  }
}

async function showScanDetail(id: number): Promise<void> {
  try {
    detailScan.value = await scanStore.getById(id)
    showDetail.value = true
  } catch {
    ElMessage.error('Failed to load scan details')
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
        <el-space>
          <el-select
            v-model="scanStore.repoId"
            placeholder="Filter by repo"
            clearable
            style="width: 240px"
            @change="scanStore.fetchList()"
          >
            <el-option
              v-for="p in projects"
              :key="p.id"
              :value="p.id"
              :label="p.name"
            />
          </el-select>
          <el-button type="primary" @click="openCreate">
            <el-icon><Plus /></el-icon> New Scan
          </el-button>
        </el-space>
      </template>
    </PageHeader>

    <el-card v-loading="scanStore.loading">
      <el-table :data="scanStore.items" row-key="id">
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="Branch" prop="branch" width="140" />
        <el-table-column label="Mode" prop="mode" width="80">
          <template #default="{ row }">
            <el-tag>{{ row.mode }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Status" prop="status" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType[row.status] ?? ''">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Findings" prop="findingsCount" width="90" />
        <el-table-column label="Started" prop="startedAt" width="170">
          <template #default="{ row }">
            {{ row.startedAt ? new Date(row.startedAt).toLocaleString() : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="Finished" prop="finishedAt" width="170">
          <template #default="{ row }">
            {{ row.finishedAt ? new Date(row.finishedAt).toLocaleString() : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="120">
          <template #default="{ row }">
            <el-space>
              <el-button size="small" @click="showScanDetail(row.id)">
                <el-icon><View /></el-icon>
              </el-button>
              <el-popconfirm
                v-if="row.status === 'pending' || row.status === 'running'"
                title="Cancel this scan?"
                @confirm="handleCancel(row.id)"
              >
                <template #reference>
                  <el-button size="small" type="danger">
                    <el-icon><CircleClose /></el-icon>
                  </el-button>
                </template>
              </el-popconfirm>
            </el-space>
          </template>
        </el-table-column>
      </el-table>
      <div style="display: flex; justify-content: flex-end; margin-top: 12px">
        <el-pagination
          v-model:current-page="scanStore.page"
          v-model:page-size="scanStore.pageSize"
          :total="scanStore.total"
          layout="total, sizes, prev, pager, next"
          @current-change="() => scanStore.fetchList()"
          @size-change="() => scanStore.fetchList()"
        />
      </div>
    </el-card>

    <!-- Create Scan Modal -->
    <el-dialog
      v-model="showCreateModal"
      title="New Scan"
      :close-on-click-modal="false"
    >
      <el-form :model="createForm" label-position="top">
        <el-form-item label="Repository" :required="true">
          <el-select v-model="createForm.repoId" placeholder="Select a repository">
            <el-option v-for="p in projects" :key="p.id" :value="p.id" :label="p.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="Mode">
          <el-select v-model="createForm.mode">
            <el-option value="full" label="Full Scan" />
            <el-option value="mr" label="MR Diff Scan" />
          </el-select>
        </el-form-item>
        <el-form-item label="Branch">
          <el-input v-model="createForm.branch" placeholder="main" />
        </el-form-item>
        <el-form-item label="Commit SHA (optional)">
          <el-input v-model="createForm.commitSha" placeholder="Specific commit to scan" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateModal = false">Cancel</el-button>
        <el-button type="primary" @click="handleCreate">Start Scan</el-button>
      </template>
    </el-dialog>

    <!-- Scan Detail Drawer -->
    <el-drawer
      v-model="showDetail"
      title="Scan Details"
      :size="520"
    >
      <template v-if="detailScan">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="ID">{{ detailScan.id }}</el-descriptions-item>
          <el-descriptions-item label="Repo ID">{{ detailScan.repoId }}</el-descriptions-item>
          <el-descriptions-item label="Branch">{{ detailScan.branch }}</el-descriptions-item>
          <el-descriptions-item label="Commit SHA">{{ detailScan.commitSha }}</el-descriptions-item>
          <el-descriptions-item label="Status">
            <el-tag :type="statusType[detailScan.status] ?? ''">{{ detailScan.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Mode">{{ detailScan.mode }}</el-descriptions-item>
          <el-descriptions-item label="Engine">{{ detailScan.engine }}</el-descriptions-item>
          <el-descriptions-item label="Findings">{{ detailScan.findingsCount }}</el-descriptions-item>
          <el-descriptions-item label="Started">{{ detailScan.startedAt ? new Date(detailScan.startedAt).toLocaleString() : '-' }}</el-descriptions-item>
          <el-descriptions-item label="Finished">{{ detailScan.finishedAt ? new Date(detailScan.finishedAt).toLocaleString() : '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="detailScan.errorMessage" label="Error">
            <span style="color: var(--el-color-danger)">{{ detailScan.errorMessage }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.cs-page :deep(.el-table) {
  overflow-x: auto;
}
.cs-page :deep(.el-dialog__body) {
  padding-top: var(--cs-space-4);
}
.cs-page :deep(.el-drawer__body) {
  padding: var(--cs-space-5);
}
@media (max-width: 768px) {
  .cs-page :deep(.el-table) {
    display: block;
    width: 100%;
  }
  .cs-page :deep(.el-space) {
    flex-wrap: wrap;
  }
}
</style>
