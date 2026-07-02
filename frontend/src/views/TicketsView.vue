<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Sort, Timer, Download, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/common/PageHeader.vue'
import { useTicketStore } from '@/stores/ticket'
import type { TicketResponse, TicketHistoryItem, TicketTransitionRequest, TicketStatus } from '@/types/ticket'
import { TICKET_STATUS_LABEL } from '@/types/ticket'

const ticketStore = useTicketStore()

type StatusType = '' | 'primary' | 'success' | 'danger' | 'warning' | 'info'
const statusType: Record<string, StatusType> = {
  pending_scan: '',
  pending_audit: 'primary',
  confirmed: 'warning',
  false_positive: '',
  pending_fix: 'primary',
  pending_retest: 'info',
  fixing: 'primary',
  closed: '',
}

// Detail drawer
const showDetail = ref(false)
const detailTicket = ref<TicketResponse | null>(null)
const historyItems = ref<TicketHistoryItem[]>([])

// Transition dialog
const showTransitionModal = ref(false)
const transitionForm = ref<TicketTransitionRequest>({ toStatus: 'pending_fix', comment: '' })
const transitionTicketId = ref<number>(0)

async function openDetail(id: number): Promise<void> {
  try {
    detailTicket.value = await ticketStore.getById(id)
    historyItems.value = await ticketStore.getHistory(id)
    showDetail.value = true
  } catch {
    ElMessage.error('Failed to load ticket details')
  }
}

function openTransition(ticket: TicketResponse): void {
  transitionTicketId.value = ticket.id
  transitionForm.value = { toStatus: 'pending_fix', comment: '' }
  showTransitionModal.value = true
}

async function handleTransition(): Promise<void> {
  try {
    await ticketStore.transition(transitionTicketId.value, transitionForm.value)
    ElMessage.success('Status updated')
    showTransitionModal.value = false
    if (showDetail.value && detailTicket.value) {
      await openDetail(detailTicket.value.id)
    }
  } catch {
    ElMessage.error('Failed to update status')
  }
}

function handleExport(id: number): void {
  window.open(`/api/v1/tickets/${id}/export`, '_blank')
}

onMounted(() => ticketStore.fetchList())
</script>

<template>
  <div class="cs-page">
    <PageHeader title="Tickets" subtitle="Vulnerability tracking and remediation workflow">
      <template #extra>
        <el-select
          v-model="ticketStore.statusFilter"
          placeholder="Filter by status"
          clearable
          style="width: 200px"
          @change="ticketStore.fetchList()"
        >
          <el-option value="" label="All statuses" />
          <el-option v-for="(label, key) in TICKET_STATUS_LABEL" :key="key" :value="key" :label="label" />
        </el-select>
      </template>
    </PageHeader>

    <el-card v-loading="ticketStore.loading">
      <el-table :data="ticketStore.items" row-key="id">
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="Vuln ID" prop="vulnId" width="80" />
        <el-table-column label="Status" prop="status" width="110">
          <template #default="{ row }">
            <span v-memo="[row.status]">
              <el-tag :type="statusType[row.status] ?? ''">
                {{ TICKET_STATUS_LABEL[row.status as TicketStatus] ?? row.status }}
              </el-tag>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="Severity" prop="severity" width="90">
          <template #default="{ row }">
            <el-tag>{{ row.severity }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Assignee" prop="assigneeName" width="120">
          <template #default="{ row }">
            {{ row.assigneeName ?? 'Unassigned' }}
          </template>
        </el-table-column>
        <el-table-column label="Deadline" prop="deadline" width="110">
          <template #default="{ row }">
            {{ row.deadline ? new Date(row.deadline).toLocaleDateString() : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="Updated" prop="updatedAt" width="170">
          <template #default="{ row }">
            {{ new Date(row.updatedAt).toLocaleString() }}
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="200">
          <template #default="{ row }">
            <span v-memo="[row.id]">
              <el-button size="small" @click="openDetail(row.id)">
                <el-icon><Timer /></el-icon> Detail
              </el-button>
              <el-button size="small" @click="openTransition(row)">
                <el-icon><Sort /></el-icon> Transition
              </el-button>
              <el-button size="small" @click="handleExport(row.id)">
                <el-icon><Download /></el-icon>
              </el-button>
            </span>
          </template>
        </el-table-column>
      </el-table>
      <div style="display: flex; justify-content: flex-end; margin-top: 12px">
        <el-pagination
          v-model:current-page="ticketStore.page"
          v-model:page-size="ticketStore.pageSize"
          :total="ticketStore.total"
          layout="total, sizes, prev, pager, next"
          @current-change="() => ticketStore.fetchList()"
          @size-change="() => ticketStore.fetchList()"
        />
      </div>
    </el-card>

    <!-- Ticket Detail Drawer -->
    <el-drawer
      v-model="showDetail"
      :title="`Ticket #${detailTicket?.id}`"
      :size="520"
    >
      <template v-if="detailTicket">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="Vuln ID">{{ detailTicket.vulnId }}</el-descriptions-item>
          <el-descriptions-item label="Status">
            <el-tag :type="statusType[detailTicket.status] ?? ''">{{ detailTicket.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Severity">{{ detailTicket.severity }}</el-descriptions-item>
          <el-descriptions-item label="Assignee">{{ detailTicket.assigneeName ?? 'Unassigned' }}</el-descriptions-item>
          <el-descriptions-item label="Deadline">{{ detailTicket.deadline ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="Created">{{ new Date(detailTicket.createdAt).toLocaleString() }}</el-descriptions-item>
          <el-descriptions-item label="Updated">{{ new Date(detailTicket.updatedAt).toLocaleString() }}</el-descriptions-item>
        </el-descriptions>

        <div style="margin-top: var(--cs-space-6)">
          <h5 style="margin: 0 0 var(--cs-space-3)">History</h5>
          <el-timeline v-if="historyItems.length">
            <el-timeline-item
              v-for="h in historyItems"
              :key="h.id"
              :timestamp="new Date(h.operatedAt).toLocaleString()"
              placement="top"
            >
              {{ h.fromStatus || '?' }} → {{ h.toStatus }}
              <span v-if="h.comment" style="color: var(--cs-text-tertiary)"> — {{ h.comment }}</span>
            </el-timeline-item>
          </el-timeline>
          <span v-else style="color: var(--cs-text-tertiary)">No history available</span>
        </div>
      </template>
    </el-drawer>

    <!-- Transition Modal -->
    <el-dialog
      v-model="showTransitionModal"
      title="Transition Ticket Status"
    >
      <el-form :model="transitionForm" label-position="top">
        <el-form-item label="Target Status" :required="true">
          <el-select v-model="transitionForm.toStatus">
            <el-option v-for="(label, key) in TICKET_STATUS_LABEL" :key="key" :value="key" :label="label" />
          </el-select>
        </el-form-item>
        <el-form-item label="Comment">
          <el-input v-model="transitionForm.comment" type="textarea" :rows="3" placeholder="Optional comment" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showTransitionModal = false">Cancel</el-button>
        <el-button type="primary" @click="handleTransition">Update</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
</style>
