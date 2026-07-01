<script setup lang="ts">
import { ref, h, onMounted } from 'vue'
import {
  Card, Table, Tag, Button, Switch, Modal, Form, Input, Select,
  Space, Typography, message, Alert, Row, Col
} from 'ant-design-vue'
import {
  SyncOutlined, PlusOutlined, DeleteOutlined, SearchOutlined
} from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { http } from '@/api/client'

interface RuleItem {
  id: number
  ruleId: string
  name: string
  severity: string
  cwe: string
  language: string
  engine: string
  detectionType: string
  description: string
  enabled: boolean
  updatedAt: string
}

interface ExemptionItem {
  id: number
  projectId: number
  ruleId: number
  ruleName: string
  ruleSeverity: string
  reason: string
  createdBy: string
  createdAt: string
  expiresAt: string | null
}

const rules = ref<RuleItem[]>([])
const loading = ref(false)
const syncing = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const filterSeverity = ref<string | undefined>(undefined)
const filterLanguage = ref<string | undefined>(undefined)

const showExemptionDialog = ref(false)
const exemptionProjectId = ref<number>(0)
const exemptionRuleId = ref<number>(0)
const exemptionReason = ref('')
const exemptionLoading = ref(false)
const exemptions = ref<ExemptionItem[]>([])
const showExemptionList = ref(false)
const exemptionListLoading = ref(false)

const severityColors: Record<string, string> = {
  critical: 'red', high: 'orange', medium: 'gold', low: 'blue', info: 'default',
}

onMounted(() => loadRules())

async function loadRules() {
  loading.value = true
  try {
    const params: Record<string, string | number> = { page: page.value, size: pageSize.value }
    if (filterSeverity.value) params.severity = filterSeverity.value
    if (filterLanguage.value) params.language = filterLanguage.value
    const resp = await http.get<{ items: RuleItem[]; total: number }>('/rules', { params })
    rules.value = resp.data.items
    total.value = resp.data.total
  } catch (e: any) {
    message.error(e.message || 'Failed to load rules')
  } finally {
    loading.value = false
  }
}

async function handleSync() {
  syncing.value = true
  try {
    const resp = await http.post<{ synced: number }>('/rules/sync')
    message.success(`Synced ${resp.data.synced} rules from engine`)
    await loadRules()
  } catch (e: any) {
    message.error(e.message || 'Sync failed')
  } finally {
    syncing.value = false
  }
}

async function handleToggle(rule: RuleItem) {
  try {
    await http.put(`/rules/${rule.id}`, { enabled: !rule.enabled })
    rule.enabled = !rule.enabled
    message.success(`${rule.name}: ${rule.enabled ? 'Enabled' : 'Disabled'}`)
  } catch (e: any) {
    message.error(e.message || 'Toggle failed')
  }
}

function openExemptionDialog(ruleId: number) {
  exemptionRuleId.value = ruleId
  exemptionReason.value = ''
  exemptionProjectId.value = 0
  showExemptionDialog.value = true
}

async function submitExemption() {
  if (!exemptionProjectId.value) { message.warning('Please enter a project ID'); return }
  exemptionLoading.value = true
  try {
    await http.post(`/projects/${exemptionProjectId.value}/exemptions`, {
      ruleId: exemptionRuleId.value, reason: exemptionReason.value,
    })
    message.success('Exemption added')
    showExemptionDialog.value = false
  } catch (e: any) {
    message.error(e.message || 'Failed to add exemption')
  } finally { exemptionLoading.value = false }
}

async function loadExemptions() {
  if (!exemptionProjectId.value) { message.warning('Please enter a project ID'); return }
  exemptionListLoading.value = true
  showExemptionList.value = true
  try {
    const resp = await http.get<ExemptionItem[]>(`/projects/${exemptionProjectId.value}/exemptions`)
    exemptions.value = resp.data
  } catch (e: any) {
    message.error(e.message || 'Failed to load exemptions')
  } finally { exemptionListLoading.value = false }
}

async function removeExemption(ruleId: number) {
  try {
    await http.delete(`/projects/${exemptionProjectId.value}/exemptions/${ruleId}`)
    message.success('Exemption removed')
    exemptions.value = exemptions.value.filter(e => e.ruleId !== ruleId)
  } catch (e: any) {
    message.error(e.message || 'Failed to remove exemption')
  }
}

function handleTableChange(pagination: any) {
  page.value = pagination.current
  pageSize.value = pagination.pageSize
  loadRules()
}

const sevTag = (s: string) => h(Tag, { color: severityColors[s] || 'default' }, () => s)

const columns = [
  { title: 'Rule ID', dataIndex: 'ruleId', key: 'ruleId', width: 200 },
  { title: 'Name', dataIndex: 'name', key: 'name', width: 250 },
  { title: 'Severity', dataIndex: 'severity', key: 'severity', width: 100, customRender: ({ value }: { value: string }) => sevTag(value) },
  { title: 'CWE', dataIndex: 'cwe', key: 'cwe', width: 100 },
  { title: 'Language', dataIndex: 'language', key: 'language', width: 100 },
  { title: 'Engine', dataIndex: 'engine', key: 'engine', width: 100 },
  { title: 'Type', dataIndex: 'detectionType', key: 'detectionType', width: 80 },
  { title: 'Enabled', dataIndex: 'enabled', key: 'enabled', width: 100, customRender: ({ value, record }: { value: boolean; record: RuleItem }) => h(Switch, { checked: value, onChange: () => handleToggle(record) }) },
  { title: 'Actions', key: 'actions', width: 160, customRender: ({ record }: { record: RuleItem }) => h(Button, { size: 'small', onClick: () => openExemptionDialog(record.id) }, { icon: () => h(PlusOutlined), default: () => 'Exempt' }) },
]

const exemptionColumns = [
  { title: 'Rule Name', dataIndex: 'ruleName', key: 'ruleName' },
  { title: 'Rule ID', dataIndex: 'ruleId', key: 'ruleId' },
  { title: 'Severity', dataIndex: 'ruleSeverity', key: 'ruleSeverity', customRender: ({ value }: { value: string }) => sevTag(value) },
  { title: 'Reason', dataIndex: 'reason', key: 'reason' },
  { title: 'Created By', dataIndex: 'createdBy', key: 'createdBy' },
  { title: 'Created At', dataIndex: 'createdAt', key: 'createdAt' },
  { title: 'Actions', key: 'actions', customRender: ({ record }: { record: ExemptionItem }) => h(Button, { danger: true, size: 'small', onClick: () => removeExemption(record.ruleId) }, { icon: () => h(DeleteOutlined), default: () => 'Remove' }) },
]
</script>

<template>
  <div class="cs-rules">
    <PageHeader title="Rule Management" subtitle="Manage detection rules and project-level exemptions" />
    <Card :bordered="false">
      <template #title><Space><span>Detection Rules</span><Tag v-if="total > 0">{{ total }} rules</Tag></Space></template>
      <template #extra>
        <Space>
          <Select v-model:value="filterSeverity" placeholder="Severity" allow-clear style="width: 120px" @change="loadRules">
            <Select.Option value="critical">Critical</Select.Option>
            <Select.Option value="high">High</Select.Option>
            <Select.Option value="medium">Medium</Select.Option>
            <Select.Option value="low">Low</Select.Option>
          </Select>
          <Select v-model:value="filterLanguage" placeholder="Language" allow-clear style="width: 120px" @change="loadRules">
            <Select.Option value="java">Java</Select.Option>
          </Select>
          <Button :loading="syncing" @click="handleSync"><template #icon><SyncOutlined /></template>Sync from Engine</Button>
        </Space>
      </template>
      <Table :dataSource="rules" :columns="columns" :loading="loading"
        :pagination="{ current: page, pageSize, total, showSizeChanger: true, showTotal: (t: number) => `${t} rules` }"
        rowKey="id" size="middle" @change="handleTableChange" />
    </Card>
    <Card :bordered="false" style="margin-top: 16px">
      <template #title><Space><span>Project Exemptions</span></Space></template>
      <Space direction="vertical" style="width: 100%">
        <Row :gutter="8">
          <Col :span="6"><Input v-model:value="exemptionProjectId" placeholder="Enter project ID" type="number" /></Col>
          <Col><Button type="primary" :loading="exemptionListLoading" @click="loadExemptions"><template #icon><SearchOutlined /></template>View Exemptions</Button></Col>
        </Row>
        <div v-if="showExemptionList">
          <Table v-if="exemptions.length > 0" :dataSource="exemptions" :columns="exemptionColumns" rowKey="id" size="small" :pagination="false" />
          <Alert v-else type="info" message="No exemptions found for this project" show-icon />
        </div>
      </Space>
    </Card>
    <Modal v-model:visible="showExemptionDialog" title="Add Project Exemption" :confirm-loading="exemptionLoading" @ok="submitExemption">
      <Form layout="vertical">
        <Form.Item label="Rule ID"><Input :value="exemptionRuleId" disabled /></Form.Item>
        <Form.Item label="Project ID" required><Input v-model:value="exemptionProjectId" placeholder="Enter project ID" type="number" /></Form.Item>
        <Form.Item label="Reason"><Input.TextArea v-model:value="exemptionReason" placeholder="Why is this rule exempted?" :rows="3" /></Form.Item>
        <Form.Item label="Expires At"><Input placeholder="Coming soon" disabled /></Form.Item>
      </Form>
    </Modal>
  </div>
</template>

<style scoped>
.cs-rules { max-width: 1400px; }
</style>
