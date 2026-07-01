<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Row,
  Col,
  Card,
  Button,
  Form,
  Input,
  Typography,
  Tag,
  Space,
  Divider,
  Collapse,
  message,
  Empty,
} from 'ant-design-vue'
import { LeftOutlined, SaveOutlined, CodeOutlined, FileSearchOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import VulnLineMarker from '@/components/code/VulnLineMarker.vue'
import CodeViewer from '@/components/code/CodeViewer.vue'
import SeverityTag from '@/components/vuln/SeverityTag.vue'
import ExploitabilityBadge from '@/components/vuln/ExploitabilityBadge.vue'
import AuditActionPanel from '@/components/audit/AuditActionPanel.vue'
import ExploitConditionForm from '@/components/audit/ExploitConditionForm.vue'
import PocUploader from '@/components/audit/PocUploader.vue'
import FixSnippetEditor from '@/components/audit/FixSnippetEditor.vue'
import AuditHistoryTimeline from '@/components/audit/AuditHistoryTimeline.vue'
import { useVulnStore } from '@/stores/vuln'
import { useAuditStore } from '@/stores/audit'
import { http } from '@/api/client'
import type { Vuln } from '@/types/vuln'
import { STATUS_LABEL, LANGUAGE_TO_MONACO } from '@/types/vuln'
import type { AuditAction, PocAttachment, AuditRecord } from '@/types/audit'
import dayjs from 'dayjs'
import type { RepoListItem } from '@/api/types'

const route = useRoute()
const router = useRouter()
const vulnStore = useVulnStore()
const auditStore = useAuditStore()

const vuln = ref<Vuln | null>(null)
const history = ref<AuditRecord[]>([])
const historyLoading = ref<boolean>(false)

const projectName = ref<string>('')

const action = ref<AuditAction>('confirm')
const exploitCondition = ref<string>('')
const impactScope = ref<string>('')
const businessScenario = ref<string>('')
const pocContent = ref<string>('')
const pocAttachments = ref<PocAttachment[]>([])
const fixSuggestion = ref<string>('')
const fixCodeSnippet = ref<string>('')

async function loadVuln(vulnId: string): Promise<void> {
  try {
    const resp = await http.get<Vuln>(`/vulns/${vulnId}`)
    vuln.value = resp.data
    vulnStore.patchVuln(resp.data)
    // Fetch project name from API (removes mock data dependency)
    try {
      const repoResp = await http.get<RepoListItem>(`/repos/${resp.data.projectId}`)
      projectName.value = repoResp.data.name
    } catch {
      projectName.value = resp.data.projectId
    }
    // Prefill fix code from the static suggestion so auditors can start typing
    fixCodeSnippet.value = resp.data.fixCodeSnippet
    fixSuggestion.value = resp.data.fixSuggestion
    // Clear form when switching between vulns
    exploitCondition.value = ''
    impactScope.value = ''
    businessScenario.value = ''
    pocContent.value = ''
    pocAttachments.value = []
  } catch (e) {
    message.error(e instanceof Error ? e.message : 'Failed to load finding')
  }
}

async function loadHistory(vulnId: string): Promise<void> {
  historyLoading.value = true
  try {
    history.value = await auditStore.fetchHistory(vulnId)
  } finally {
    historyLoading.value = false
  }
}

onMounted(async () => {
  const id = route.params.vulnId
  if (typeof id !== 'string') return
  await Promise.all([loadVuln(id), loadHistory(id)])
})

watch(
  () => route.params.vulnId,
  (newId, oldId) => {
    if (typeof newId === 'string' && newId !== oldId) {
      void loadVuln(newId)
      void loadHistory(newId)
    }
  },
)

const formValid = computed<boolean>(() => {
  if (vuln.value === null) return false
  if (exploitCondition.value.trim().length === 0) return false
  if (action.value === 'confirm' && fixCodeSnippet.value.trim().length === 0) return false
  return true
})

async function handleSubmit(): Promise<void> {
  if (vuln.value === null) return
  if (!formValid.value) {
    message.warning('Exploit condition is required; confirm action also needs a fix snippet.')
    return
  }
  try {
    const record = await auditStore.submitAudit({
      vulnId: vuln.value.id,
      action: action.value,
      exploitCondition: exploitCondition.value,
      pocContent: pocContent.value,
      pocAttachments: pocAttachments.value,
      impactScope: impactScope.value,
      businessScenario: businessScenario.value,
      fixSuggestion: fixSuggestion.value,
      fixCodeSnippet: fixCodeSnippet.value,
      fixLanguage: LANGUAGE_TO_MONACO[vuln.value.fixLanguage],
    })
    message.success(
      action.value === 'confirm'
        ? 'Vulnerability confirmed; ticket assigned to fixing owner.'
        : action.value === 'false_positive'
          ? 'Marked as false positive; closed.'
          : 'Retest requested; team has been notified.',
    )
    // Patch the vuln store so the queue reflects new status
    if (vuln.value !== null) {
      const updated: Vuln = {
        ...vuln.value,
        status: record.resultingStatus,
        exploitability: record.resultingExploitability ?? vuln.value.exploitability,
      }
      vulnStore.patchVuln(updated)
      vuln.value = updated
    }
    await loadHistory(vuln.value.id)
  } catch (e) {
    message.error(e instanceof Error ? e.message : 'Failed to submit audit')
  }
}

async function downloadPdf(): Promise<void> {
  if (vuln.value === null) return
  try {
    const resp = await http.get(`/vulns/${vuln.value.id}/export`, { responseType: 'blob' })
    const blob = new Blob([resp.data], { type: 'application/pdf' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `vuln-${vuln.value.id}.pdf`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch (e) {
    message.error(e instanceof Error ? e.message : 'Failed to download PDF')
  }
}

function goBack(): void {
  router.push('/audit')
}
</script>

<template>
  <div v-if="vuln !== null" class="cs-workbench">
    <PageHeader
      :title="vuln.title"
      :subtitle="`${vuln.ruleId} · ${vuln.cwe}${vuln.cve !== null ? ' · ' + vuln.cve : ''}`"
    >
      <template #extra>
        <Space :size="8">
          <Button @click="goBack"><LeftOutlined /> Queue</Button>
          <Button @click="downloadPdf"><DownloadOutlined /> PDF</Button>
          <Button type="primary" :loading="auditStore.submitting" :disabled="!formValid" @click="handleSubmit">
            <SaveOutlined /> Submit audit
          </Button>
        </Space>
      </template>
    </PageHeader>

    <Card :bordered="false" class="cs-workbench__exploitCard">
      <div class="cs-workbench__exploitRow">
        <ExploitabilityBadge :exploitability="vuln.exploitability" :reason="vuln.exploitReason" prominent />
        <div class="cs-workbench__exploitMeta">
          <Typography.Text type="secondary" class="cs-workbench__exploitLabel">判定理由</Typography.Text>
          <Typography.Paragraph v-if="vuln.exploitReason.length > 0" class="cs-workbench__exploitReason">
            {{ vuln.exploitReason }}
          </Typography.Paragraph>
          <Typography.Text v-else type="secondary" class="cs-workbench__exploitReasonEmpty">
            未提供判定理由
          </Typography.Text>
        </div>
      </div>
    </Card>

    <Row :gutter="[16, 16]" class="cs-workbench__top">
      <Col :xs="24" :lg="14">
        <Card
          :bordered="false"
          class="cs-workbench__codeCard"
          :body-style="{ padding: 0, height: 'calc(100vh - 280px)' }"
        >
          <template #title>
            <Space :size="6">
              <CodeOutlined />
              <span>Source</span>
              <Tag bordered color="purple">{{ projectName }}</Tag>
            </Space>
          </template>
          <template #extra>
            <Typography.Text type="secondary" class="cs-workbench__lang">
              {{ vuln.filePath }}
            </Typography.Text>
          </template>
          <div class="cs-workbench__codeShell">
            <VulnLineMarker :vuln="vuln" />
            <div class="cs-workbench__code">
              <CodeViewer
                :code="vuln.codeSnippet"
                :language="vuln.fixLanguage"
                :vuln="vuln"
                height="calc(100% - 56px)"
              />
            </div>
          </div>
        </Card>
      </Col>
      <Col :xs="24" :lg="10">
        <Card :bordered="false" class="cs-workbench__panel" :body-style="{ padding: 'var(--cs-space-4)' }">
          <div class="cs-workbench__panelHead">
            <Space :size="6" wrap>
              <SeverityTag :severity="vuln.severity" size="sm" />
              <Tag bordered color="default">{{ STATUS_LABEL[vuln.status] }}</Tag>
              <ExploitabilityBadge :exploitability="vuln.exploitability" :reason="vuln.exploitReason" />
            </Space>
            <Typography.Text type="secondary" class="cs-workbench__meta">
              {{ projectName }} · {{ vuln.discoveredBy }} · {{ dayjs(vuln.discoveredAt).format('MMM D, HH:mm') }}
            </Typography.Text>
          </div>

          <Divider style="margin: 12px 0" />

          <Typography.Paragraph class="cs-workbench__desc">
            {{ vuln.description }}
          </Typography.Paragraph>

          <Form layout="vertical" class="cs-workbench__form">
            <div class="cs-workbench__section">
              <Typography.Text class="cs-workbench__sectionLabel">Decision</Typography.Text>
              <AuditActionPanel v-model="action" />
            </div>

            <div class="cs-workbench__section">
              <Typography.Text class="cs-workbench__sectionLabel">Exploit context</Typography.Text>
              <ExploitConditionForm
                v-model:exploitCondition="exploitCondition"
                v-model:impactScope="impactScope"
                v-model:businessScenario="businessScenario"
              />
            </div>

            <div class="cs-workbench__section">
              <Typography.Text class="cs-workbench__sectionLabel">Proof of concept</Typography.Text>
              <PocUploader v-model="pocAttachments" v-model:content="pocContent" />
            </div>

            <Collapse :bordered="false" class="cs-workbench__collapse">
              <Collapse.Panel key="fix" header="Standard fix snippet">
                <Form.Item label="Fix description" :label-col="{ span: 24 }" :wrapper-col="{ span: 24 }">
                  <Input.TextArea
                    v-model:value="fixSuggestion"
                    :rows="2"
                    placeholder="A short, prescriptive fix instruction developers can follow."
                  />
                </Form.Item>
                <FixSnippetEditor v-model="fixCodeSnippet" :language="vuln.fixLanguage" height="200px" />
              </Collapse.Panel>
            </Collapse>
          </Form>
        </Card>
      </Col>
    </Row>

    <Card :bordered="false" class="cs-workbench__historyCard" style="margin-top: 16px">
      <template #title>
        <Space :size="6">
          <FileSearchOutlined />
          <span>Audit history</span>
          <Tag bordered color="default">{{ history.length }} actions</Tag>
        </Space>
      </template>
      <AuditHistoryTimeline :records="history" :loading="historyLoading" />
    </Card>
  </div>
  <div v-else class="cs-workbench__loading">
    <Empty description="Loading finding…" />
  </div>
</template>

<style scoped>
.cs-workbench__exploitCard {
  background: var(--cs-bg-elevated);
  border: 1px solid var(--cs-border-light);
  border-radius: var(--cs-radius-lg);
  margin-bottom: var(--cs-space-4);
}
.cs-workbench__exploitCard :deep(.ant-card-body) {
  padding: var(--cs-space-3) var(--cs-space-4);
}
.cs-workbench__exploitRow {
  display: flex;
  align-items: flex-start;
  gap: var(--cs-space-3);
  flex-wrap: wrap;
}
.cs-workbench__exploitMeta {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.cs-workbench__exploitLabel {
  font-size: var(--cs-font-size-xs);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  font-weight: 600;
}
.cs-workbench__exploitReason {
  margin-bottom: 0 !important;
  color: var(--cs-text-primary);
  font-size: var(--cs-font-size-sm);
  line-height: var(--cs-line-height-relaxed);
}
.cs-workbench__exploitReasonEmpty {
  font-size: var(--cs-font-size-sm);
  font-style: italic;
}
.cs-workbench__codeCard {
  background: var(--cs-bg-elevated);
  border: 1px solid var(--cs-border-light);
  border-radius: var(--cs-radius-lg);
  height: calc(100vh - 240px);
  display: flex;
  flex-direction: column;
}
.cs-workbench__codeCard :deep(.ant-card-body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 0;
}
.cs-workbench__codeShell {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: var(--cs-space-3);
  gap: var(--cs-space-2);
  min-height: 0;
}
.cs-workbench__code {
  flex: 1;
  min-height: 0;
}
.cs-workbench__panel {
  background: var(--cs-bg-elevated);
  border: 1px solid var(--cs-border-light);
  border-radius: var(--cs-radius-lg);
  max-height: calc(100vh - 240px);
  overflow-y: auto;
}
.cs-workbench__panelHead {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cs-space-2);
  flex-wrap: wrap;
}
.cs-workbench__meta {
  font-size: var(--cs-font-size-xs);
  font-family: var(--cs-font-mono);
}
.cs-workbench__lang {
  font-family: var(--cs-font-mono);
  font-size: 11px;
}
.cs-workbench__desc {
  font-size: var(--cs-font-size-sm);
  color: var(--cs-text-secondary);
  line-height: var(--cs-line-height-relaxed);
  margin-bottom: 0 !important;
}
.cs-workbench__form {
  margin-top: var(--cs-space-2);
}
.cs-workbench__section {
  margin-bottom: var(--cs-space-4);
}
.cs-workbench__sectionLabel {
  display: block;
  font-size: var(--cs-font-size-sm);
  font-weight: 600;
  color: var(--cs-text-primary);
  margin-bottom: var(--cs-space-2);
}
.cs-workbench__collapse {
  background: transparent !important;
  border: none !important;
}
.cs-workbench__collapse :deep(.ant-collapse-item) {
  border: 1px solid var(--cs-border-light) !important;
  border-radius: var(--cs-radius-md) !important;
  background: var(--cs-bg-sunken) !important;
  margin-bottom: var(--cs-space-2);
}
.cs-workbench__collapse :deep(.ant-collapse-header) {
  font-weight: 600;
  font-size: var(--cs-font-size-sm);
  color: var(--cs-text-primary);
}
.cs-workbench__historyCard {
  background: var(--cs-bg-elevated);
  border: 1px solid var(--cs-border-light);
  border-radius: var(--cs-radius-lg);
}
.cs-workbench__loading {
  padding: var(--cs-space-12);
  text-align: center;
}
</style>
