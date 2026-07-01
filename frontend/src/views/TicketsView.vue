<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  Card, Table, Button, Tag, Space, Typography, Select, message,
  Drawer, Descriptions, Timeline, Modal, Form, Input,
} from 'ant-design-vue'
import {
  SwapOutlined, HistoryOutlined, FilePdfOutlined, UserSwitchOutlined,
} from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { useTicketStore } from '@/stores/ticket'
import type { TicketResponse, TicketHistoryItem, TicketTransitionRequest, TicketStatus } from '@/types/ticket'
import { TICKET_STATUS_LABEL } from '@/types/ticket'

const ticketStore = useTicketStore()

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 70 },
  { title: 'Vuln ID', dataIndex: 'vulnId', key: 'vulnId', width: 80 },
  { title: 'Status', dataIndex: 'status', key: 'status', width: 110 },
  { title: 'Severity', dataIndex: 'severity', key: 'severity', width: 90 },
  { title: 'Assignee', dataIndex: 'assigneeName', key: 'assigneeName', width: 120 },
  { title: 'Deadline', dataIndex: 'deadline', key: 'deadline', width: 110 },
  { title: 'Updated', dataIndex: 'updatedAt', key: 'updatedAt', width: 170 },
  { title: 'Actions', key: 'actions', width: 160 },
]

const statusColor: Record<string, string> = {
  open: 'blue',
  in_progress: 'processing',
  fixed: 'success',
  closed: 'default',
  waived: 'warning',
  rejected: 'error',
  retest: 'purple',
}

// Detail drawer
const showDetail = ref(false)
const detailTicket = ref<TicketResponse | null>(null)
const historyItems = ref<TicketHistoryItem[]>([])

// Transition dialog
const showTransitionModal = ref(false)
const transitionTarget = ref<TicketStatus>('in_progress')
const transitionForm = ref<TicketTransitionRequest>({ toStatus: 'in_progress' })
const transitionTicketId = ref<number>(0)

async function openDetail(id: number): Promise<void> {
  try {
    detailTicket.value = await ticketStore.getById(id)
    historyItems.value = await ticketStore.getHistory(id)
    showDetail.value = true
  } catch {
    message.error('Failed to load ticket details')
  }
}

function openTransition(ticket: TicketResponse): void {
  transitionTicketId.value = ticket.id
  transitionForm.value = { toStatus: 'in_progress', comment: '' }
  showTransitionModal.value = true
}

async function handleTransition(): Promise<void> {
  try {
    await ticketStore.transition(transitionTicketId.value, transitionForm.value)
    message.success('Status updated')
    showTransitionModal.value = false
    // Refresh detail if open
    if (showDetail.value && detailTicket.value) {
      await openDetail(detailTicket.value.id)
    }
  } catch {
    message.error('Failed to update status')
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
        <Select
          v-model:value="ticketStore.statusFilter"
          placeholder="Filter by status"
          allow-clear
          style="width: 200px"
          @change="ticketStore.fetchList()"
        >
          <Select.Option value="">All statuses</Select.Option>
          <Select.Option v-for="(label, key) in TICKET_STATUS_LABEL" :key="key" :value="key">
            {{ label }}
          </Select.Option>
        </Select>
      </template>
    </PageHeader>

    <Card :loading="ticketStore.loading">
      <Table
        :data-source="ticketStore.items"
        :columns="columns"
        :pagination="{
          total: ticketStore.total,
          current: ticketStore.page,
          pageSize: ticketStore.pageSize,
          showSizeChanger: true,
          showTotal: (t: number) => `Total ${t} items`,
          onChange: (p: number) => { ticketStore.setPage(p); ticketStore.fetchList() },
        }"
        row-key="id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <Tag :color="statusColor[record.status] ?? 'default'">
              {{ TICKET_STATUS_LABEL[record.status as TicketStatus] ?? record.status }}
            </Tag>
          </template>
          <template v-if="column.key === 'severity'">
            <Tag>{{ record.severity }}</Tag>
          </template>
          <template v-if="column.key === 'assigneeName'">
            {{ record.assigneeName ?? 'Unassigned' }}
          </template>
          <template v-if="column.key === 'deadline'">
            {{ record.deadline ? new Date(record.deadline).toLocaleDateString() : '-' }}
          </template>
          <template v-if="column.key === 'updatedAt'">
            {{ new Date(record.updatedAt).toLocaleString() }}
          </template>
          <template v-if="column.key === 'actions'">
            <Space>
              <Button size="small" @click="openDetail((record as any).id)">
                <template #icon><HistoryOutlined /></template>
                Detail
              </Button>
              <Button size="small" @click="openTransition((record as any))">
                <template #icon><SwapOutlined /></template>
                Transition
              </Button>
              <Button size="small" @click="handleExport((record as any).id)">
                <template #icon><FilePdfOutlined /></template>
              </Button>
            </Space>
          </template>
        </template>
      </Table>
    </Card>

    <!-- Ticket Detail Drawer -->
    <Drawer
      v-model:open="showDetail"
      :title="`Ticket #${detailTicket?.id}`"
      placement="right"
      width="520"
    >
      <template v-if="detailTicket">
        <Descriptions :column="1" bordered size="small">
          <Descriptions.Item label="Vuln ID">{{ detailTicket.vulnId }}</Descriptions.Item>
          <Descriptions.Item label="Status">
            <Tag :color="statusColor[detailTicket.status] ?? 'default'">{{ detailTicket.status }}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="Severity">{{ detailTicket.severity }}</Descriptions.Item>
          <Descriptions.Item label="Assignee">{{ detailTicket.assigneeName ?? 'Unassigned' }}</Descriptions.Item>
          <Descriptions.Item label="Deadline">{{ detailTicket.deadline ?? '-' }}</Descriptions.Item>
          <Descriptions.Item label="Created">{{ new Date(detailTicket.createdAt).toLocaleString() }}</Descriptions.Item>
          <Descriptions.Item label="Updated">{{ new Date(detailTicket.updatedAt).toLocaleString() }}</Descriptions.Item>
        </Descriptions>

        <div style="margin-top: 24px">
          <Typography.Title :level="5">History</Typography.Title>
          <Timeline v-if="historyItems.length">
            <Timeline.Item v-for="h in historyItems" :key="h.id">
              <Typography.Text type="secondary">{{ new Date(h.operatedAt).toLocaleString() }}</Typography.Text>
              <br />
              {{ h.fromStatus || '?' }} → {{ h.toStatus }}
              <Typography.Text v-if="h.comment" type="secondary"> — {{ h.comment }}</Typography.Text>
            </Timeline.Item>
          </Timeline>
          <Typography.Text v-else type="secondary">No history available</Typography.Text>
        </div>
      </template>
    </Drawer>

    <!-- Transition Modal -->
    <Modal
      v-model:open="showTransitionModal"
      title="Transition Ticket Status"
      @ok="handleTransition"
      ok-text="Update"
    >
      <Form :model="transitionForm" layout="vertical">
        <Form.Item label="Target Status" :required="true">
          <Select v-model:value="transitionForm.toStatus">
            <Select.Option v-for="(label, key) in TICKET_STATUS_LABEL" :key="key" :value="key">
              {{ label }}
            </Select.Option>
          </Select>
        </Form.Item>
        <Form.Item label="Comment">
          <Input.TextArea v-model:value="transitionForm.comment" :rows="3" placeholder="Optional comment" />
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
