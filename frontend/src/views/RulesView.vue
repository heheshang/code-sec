<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Refresh, Plus, Delete, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/common/PageHeader.vue'
import { http } from '@/api/client'
import { errMsg } from '@/utils/error'

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

type SeverityType = '' | 'primary' | 'success' | 'danger' | 'warning' | 'info'
const severityType: Record<string, SeverityType> = {
  critical: 'danger', high: 'warning', medium: 'warning', low: 'primary', info: '',
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
  } catch (e: unknown) {
    ElMessage.error(errMsg(e))
  } finally {
    loading.value = false
  }
}

async function handleSync() {
  syncing.value = true
  try {
    const resp = await http.post<{ synced: number }>('/rules/sync')
    ElMessage.success(`Synced ${resp.data.synced} rules from engine`)
    await loadRules()
  } catch (e: unknown) {
    ElMessage.error(errMsg(e))
  } finally {
    syncing.value = false
  }
}

async function handleToggle(rule: RuleItem) {
  try {
    await http.put(`/rules/${rule.id}`, { enabled: !rule.enabled })
    rule.enabled = !rule.enabled
    ElMessage.success(`${rule.name}: ${rule.enabled ? 'Enabled' : 'Disabled'}`)
  } catch (e: unknown) {
    ElMessage.error(errMsg(e))
  }
}

function openExemptionDialog(ruleId: number) {
  exemptionRuleId.value = ruleId
  exemptionReason.value = ''
  exemptionProjectId.value = 0
  showExemptionDialog.value = true
}

async function submitExemption() {
  if (!exemptionProjectId.value) { ElMessage.warning('Please enter a project ID'); return }
  exemptionLoading.value = true
  try {
    await http.post(`/projects/${exemptionProjectId.value}/exemptions`, {
      ruleId: exemptionRuleId.value, reason: exemptionReason.value,
    })
    ElMessage.success('Exemption added')
    showExemptionDialog.value = false
  } catch (e: unknown) {
    ElMessage.error(errMsg(e))
  } finally { exemptionLoading.value = false }
}

async function loadExemptions() {
  if (!exemptionProjectId.value) { ElMessage.warning('Please enter a project ID'); return }
  exemptionListLoading.value = true
  showExemptionList.value = true
  try {
    const resp = await http.get<ExemptionItem[]>(`/projects/${exemptionProjectId.value}/exemptions`)
    exemptions.value = resp.data
  } catch (e: unknown) {
    ElMessage.error(errMsg(e))
  } finally { exemptionListLoading.value = false }
}

async function removeExemption(ruleId: number) {
  try {
    await http.delete(`/projects/${exemptionProjectId.value}/exemptions/${ruleId}`)
    ElMessage.success('Exemption removed')
    exemptions.value = exemptions.value.filter(e => e.ruleId !== ruleId)
  } catch (e: unknown) {
    ElMessage.error(errMsg(e))
  }
}
</script>

<template>
  <div class="cs-page">
    <PageHeader title="Rule Management" subtitle="Manage detection rules and project-level exemptions" />
    <el-card shadow="never">
      <template #header>
        <span style="display: flex; align-items: center; justify-content: space-between; width: 100%;">
          <el-space>
            <span>Detection Rules</span>
            <el-tag v-if="total > 0" size="small">{{ total }} rules</el-tag>
          </el-space>
          <el-space>
            <el-select v-model="filterSeverity" placeholder="Severity" clearable style="width: 120px" @change="loadRules">
              <el-option value="critical" label="Critical" />
              <el-option value="high" label="High" />
              <el-option value="medium" label="Medium" />
              <el-option value="low" label="Low" />
            </el-select>
            <el-select v-model="filterLanguage" placeholder="Language" clearable style="width: 120px" @change="loadRules">
              <el-option value="java" label="Java" />
            </el-select>
            <el-button :loading="syncing" @click="handleSync">
              <el-icon><Refresh /></el-icon> Sync from Engine
            </el-button>
          </el-space>
        </span>
      </template>
      <el-table :data="rules" v-loading="loading" row-key="id">
        <el-table-column label="Rule ID" prop="ruleId" width="200" />
        <el-table-column label="Name" prop="name" width="250" />
        <el-table-column label="Severity" prop="severity" width="100">
          <template #default="{ row }">
            <el-tag :type="severityType[row.severity] ?? ''">{{ row.severity }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="CWE" prop="cwe" width="100" />
        <el-table-column label="Language" prop="language" width="100" />
        <el-table-column label="Engine" prop="engine" width="100" />
        <el-table-column label="Type" prop="detectionType" width="80" />
        <el-table-column label="Enabled" prop="enabled" width="100">
          <template #default="{ row }">
            <el-switch :model-value="!!row.enabled" @change="() => handleToggle(row as RuleItem)" />
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="160">
          <template #default="{ row }">
            <el-button size="small" @click="openExemptionDialog((row as RuleItem).id)">
              <el-icon><Plus /></el-icon> Exempt
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display: flex; justify-content: flex-end; margin-top: 12px">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @current-change="loadRules"
          @size-change="loadRules"
        />
      </div>
    </el-card>
    <el-card shadow="never" style="margin-top: var(--cs-space-4)">
      <template #header>
        <el-space><span>Project Exemptions</span></el-space>
      </template>
      <div style="display: flex; flex-direction: column; gap: var(--cs-space-3)">
        <el-row :gutter="8">
          <el-col :span="6">
            <el-input v-model="exemptionProjectId" placeholder="Enter project ID" type="number" />
          </el-col>
          <el-col>
            <el-button type="primary" :loading="exemptionListLoading" @click="loadExemptions">
              <el-icon><Search /></el-icon> View Exemptions
            </el-button>
          </el-col>
        </el-row>
        <div v-if="showExemptionList">
          <el-table v-if="exemptions.length > 0" :data="exemptions" row-key="id" size="small">
            <el-table-column label="Rule Name" prop="ruleName" />
            <el-table-column label="Rule ID" prop="ruleId" />
            <el-table-column label="Severity" prop="ruleSeverity">
              <template #default="{ row }">
                <el-tag :type="severityType[row.ruleSeverity] ?? ''">{{ row.ruleSeverity }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="Reason" prop="reason" />
            <el-table-column label="Created By" prop="createdBy" />
            <el-table-column label="Created At" prop="createdAt" />
            <el-table-column label="Actions">
              <template #default="{ row }">
                <el-button type="danger" size="small" @click="removeExemption((row as ExemptionItem).ruleId)">
                  <el-icon><Delete /></el-icon> Remove
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-alert v-else type="info" title="No exemptions found for this project" show-icon :closable="false" />
        </div>
      </div>
    </el-card>
    <el-dialog v-model="showExemptionDialog" title="Add Project Exemption" :close-on-click-modal="false">
      <el-form label-position="top">
        <el-form-item label="Rule ID">
          <el-input :model-value="exemptionRuleId" disabled />
        </el-form-item>
        <el-form-item label="Project ID" required>
          <el-input v-model="exemptionProjectId" placeholder="Enter project ID" type="number" />
        </el-form-item>
        <el-form-item label="Reason">
          <el-input v-model="exemptionReason" type="textarea" :rows="3" placeholder="Why is this rule exempted?" />
        </el-form-item>
        <el-form-item label="Expires At">
          <el-input placeholder="Coming soon" disabled />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showExemptionDialog = false">Cancel</el-button>
        <el-button type="primary" :loading="exemptionLoading" @click="submitExemption">Submit</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
</style>
