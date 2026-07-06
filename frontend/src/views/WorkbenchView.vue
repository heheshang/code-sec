<script setup lang="ts">
import { ref, computed, onMounted, watch, defineAsyncComponent } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Download, Refresh, Document, Search, Connection } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/common/PageHeader.vue'
import VulnLineMarker from '@/components/code/VulnLineMarker.vue'
import SeverityTag from '@/components/vuln/SeverityTag.vue'
import ExploitabilityBadge from '@/components/vuln/ExploitabilityBadge.vue'
import AuditActionPanel from '@/components/audit/AuditActionPanel.vue'
import ExploitConditionForm from '@/components/audit/ExploitConditionForm.vue'
import PocUploader from '@/components/audit/PocUploader.vue'
import AuditHistoryTimeline from '@/components/audit/AuditHistoryTimeline.vue'

/** Async-loaded because both components import monaco-editor, which blocks the
 *  main thread during initialization (several MB of JS parse + web worker
 *  setup).  We show a plain <pre> fallback first; once idle we upgrade to the
 *  real Monaco-based viewer. */
const CodeViewerAsync = defineAsyncComponent(() => import('@/components/code/CodeViewer.vue'))
const FixSnippetEditorAsync = defineAsyncComponent(() => import('@/components/audit/FixSnippetEditor.vue'))
import SkeletonCard from '@/components/common/SkeletonCard.vue'
import { useVulnStore } from '@/stores/vuln'
import { useAuditStore } from '@/stores/audit'
import { http } from '@/api/client'
import { errMsg } from '@/utils/error'
import type { Vuln, VulnApiResponse, Language } from '@/types/vuln'
import { STATUS_LABEL, LANGUAGE_TO_CM, vulnFromApi } from '@/types/vuln'
import type { AuditAction, PocAttachment, AuditRecord } from '@/types/audit'
import dayjs from 'dayjs'
import type { RepoListItem } from '@/api/types'
import AiAuditPanel from '@/ai-audit/components/AiAuditPanel.vue'
import CallGraphView from '@/graph/components/CallGraphView.vue'

/** Map file extension → Language for code viewer. */
const EXT_TO_LANG: Record<string, Language> = {
  '.java': 'java',
  '.kt': 'java',
  '.go': 'go',
  '.py': 'python',
  '.ts': 'typescript',
  '.tsx': 'typescript',
  '.js': 'javascript',
  '.jsx': 'javascript',
  '.php': 'php',
  '.cs': 'csharp',
}

function detectLanguage(filePath: string | undefined): Language {
  if (!filePath) return 'java'
  const ext = filePath.substring(filePath.lastIndexOf('.'))
  return EXT_TO_LANG[ext] ?? 'java'
}

const route = useRoute()
const router = useRouter()
const vulnStore = useVulnStore()
const auditStore = useAuditStore()

const vuln = ref<Vuln | null>(null)
const vulnLoading = ref(false)
const vulnError = ref<string | null>(null)
const history = ref<AuditRecord[]>([])
const historyLoading = ref<boolean>(false)

const projectName = ref<string>('')

/** Set to true once the main thread is idle, so we can swap the <pre>
 * fallback for the real Monaco-based CodeViewer without freezing the page. */
const showCodeViewer = ref(false)
/** Set to true when the fix-snippet Collapse.Panel is expanded, so we don't
 *  instantiate a second Monaco editor on every page load. */
const fixPanelOpen = ref(false)

const aiAnalyzing = ref(false)
const showAiPanel = ref(false)

async function runAiAnalysis() {
  const id = route.params.vulnId
  if (typeof id !== 'string') return
  aiAnalyzing.value = true
  showAiPanel.value = true
  try {
    await http.post(`/ai/analyze/${id}`)
    await loadVuln(id)
  } catch (e: unknown) {
    ElMessage.error(errMsg(e))
  } finally {
    aiAnalyzing.value = false
  }
}

const action = ref<AuditAction>('confirm')
const exploitCondition = ref<string>('')
const impactScope = ref<string>('')
const businessScenario = ref<string>('')
const pocContent = ref<string>('')
const pocAttachments = ref<PocAttachment[]>([])
const fixSuggestion = ref<string>('')
const fixCodeSnippet = ref<string>('')

const activeCollapse = ref<string[]>([])

async function loadVuln(vulnId: string): Promise<void> {
  vulnLoading.value = true
  vulnError.value = null
  try {
    const resp = await http.get<VulnApiResponse>(`/vulns/${vulnId}`)
    const apiVuln = resp.data
    const frontendVuln = vulnFromApi(apiVuln)
    vuln.value = frontendVuln
    vulnStore.patchVuln(frontendVuln)
    if (apiVuln.projectId && apiVuln.projectId !== 0) {
      try {
        const repoResp = await http.get<RepoListItem>(`/repos/${apiVuln.projectId}`)
        projectName.value = repoResp.data.name
      } catch {
        projectName.value = `#${apiVuln.projectId}`
      }
    } else {
      projectName.value = ''
    }
    fixCodeSnippet.value = frontendVuln.fixCodeSnippet ?? ''
    fixSuggestion.value = frontendVuln.fixSuggestion ?? ''
    exploitCondition.value = ''
    impactScope.value = ''
    businessScenario.value = ''
    pocContent.value = ''
    pocAttachments.value = []
  } catch (e: unknown) {
    vulnError.value = errMsg(e)
    ElMessage.error(vulnError.value)
  } finally {
    vulnLoading.value = false
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
  if (typeof requestIdleCallback === 'function') {
    requestIdleCallback(() => { showCodeViewer.value = true })
  } else {
    setTimeout(() => { showCodeViewer.value = true }, 300)
  }
})

watch(
  () => route.params.vulnId,
  (newId, oldId) => {
    if (typeof newId === 'string' && newId !== oldId) {
      vuln.value = null
      vulnError.value = null
      showCodeViewer.value = false
      void loadVuln(newId).then(() => {
        if (typeof requestIdleCallback === 'function') {
          requestIdleCallback(() => { showCodeViewer.value = true })
        } else {
          setTimeout(() => { showCodeViewer.value = true }, 300)
        }
      })
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
    ElMessage.warning('Exploit condition is required; confirm action also needs a fix snippet.')
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
      fixLanguage: vuln.value.fixLanguage ? LANGUAGE_TO_CM[vuln.value.fixLanguage] : 'java',
    })
    ElMessage.success(
      action.value === 'confirm'
        ? 'Vulnerability confirmed; ticket assigned to fixing owner.'
        : action.value === 'false_positive'
          ? 'Marked as false positive; closed.'
          : 'Retest requested; team has been notified.',
    )
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
  } catch (e: unknown) {
    ElMessage.error(errMsg(e))
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
  } catch (e: unknown) {
    ElMessage.error(errMsg(e))
  }
}

const codeLanguage = computed<Language>(() => {
  if (vuln.value?.fixLanguage) return vuln.value.fixLanguage
  return detectLanguage(vuln.value?.filePath)
})

function retry(): void {
  const id = route.params.vulnId
  if (typeof id !== 'string') return
  loadVuln(id)
  loadHistory(id)
}

function goBack(): void {
  if (window.history.length > 2) {
    router.back()
  } else {
    router.push('/audit')
  }
}

function onCollapseChange(keys: string[]): void {
  fixPanelOpen.value = keys.includes('fix')
}
</script>

<template>
  <div v-if="vuln !== null" class="cs-workbench">
    <PageHeader
      :title="vuln.title"
      :subtitle="`${vuln.ruleId} · ${vuln.cwe}${vuln.cve !== null ? ' · ' + vuln.cve : ''}`"
    >
      <template #extra>
        <el-space :size="8">
          <el-button @click="goBack"><el-icon><ArrowLeft /></el-icon> Queue</el-button>
          <el-button @click="downloadPdf"><el-icon><Download /></el-icon> PDF</el-button>
          <el-button type="primary" :loading="aiAnalyzing" :disabled="vuln === null" @click="runAiAnalysis">
            {{ aiAnalyzing ? 'Analyzing…' : 'AI Analyze' }}
          </el-button>
          <el-button type="primary" :loading="auditStore.submitting" :disabled="!formValid" @click="handleSubmit">
            Submit audit
          </el-button>
        </el-space>
      </template>
    </PageHeader>

    <el-card shadow="never" class="cs-workbench__exploitCard" v-memo="[vuln.exploitability, vuln.exploitReason]">
      <div class="cs-workbench__exploitRow">
        <ExploitabilityBadge :exploitability="vuln.exploitability" :reason="vuln.exploitReason" prominent />
        <div class="cs-workbench__exploitMeta">
          <span class="cs-workbench__exploitLabel">判定理由</span>
          <p v-if="(vuln.exploitReason?.length ?? 0) > 0" class="cs-workbench__exploitReason">
            {{ vuln.exploitReason }}
          </p>
          <span v-else class="cs-workbench__exploitReasonEmpty">未提供判定理由</span>
        </div>
      </div>
    </el-card>

    <el-row :gutter="16" class="cs-workbench__top">
      <el-col :xs="24" :lg="14">
        <el-card
          shadow="never"
          class="cs-workbench__codeCard"
          :body-style="{ padding: 0, height: 'calc(100vh - 280px)', minHeight: '400px' }"
        >
          <template #header>
            <span style="display: flex; align-items: center; justify-content: space-between; width: 100%;">
              <el-space :size="6">
                <el-icon><Document /></el-icon>
                <span>Source</span>
                <el-tag v-if="projectName" type="info" effect="plain">{{ projectName }}</el-tag>
              </el-space>
              <span class="cs-workbench__lang">{{ vuln.filePath }}</span>
            </span>
          </template>
          <div class="cs-workbench__codeShell">
            <VulnLineMarker :vuln="vuln" />
            <div class="cs-workbench__code">
              <CodeViewerAsync
                v-if="showCodeViewer"
                :code="vuln.codeSnippet"
                :language="codeLanguage"
                :vuln="vuln"
                height="calc(100% - 56px)"
              />
              <pre v-else class="cs-workbench__codeFallback">{{ vuln.codeSnippet }}</pre>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="cs-workbench__panel" :body-style="{ padding: 'var(--cs-space-4)' }">
          <div class="cs-workbench__panelHead">
            <el-space :size="6" wrap>
              <SeverityTag :severity="vuln.severity" size="sm" />
              <el-tag v-if="vuln.status" effect="plain">{{ STATUS_LABEL[vuln.status] ?? vuln.status }}</el-tag>
              <ExploitabilityBadge :exploitability="vuln.exploitability" :reason="vuln.exploitReason" />
            </el-space>
            <span class="cs-workbench__meta">
              {{ projectName || '—' }} · {{ vuln.discoveredBy ?? 'engine' }} · {{ dayjs(vuln.discoveredAt).format('MMM D, HH:mm') }}
            </span>
          </div>

          <el-divider style="margin: var(--cs-space-3) 0" />

          <p class="cs-workbench__desc">
            {{ vuln.description }}
          </p>

          <div class="cs-workbench__form">
            <div class="cs-workbench__section">
              <span class="cs-workbench__sectionLabel">Decision</span>
              <AuditActionPanel v-model="action" />
            </div>

            <div class="cs-workbench__section">
              <span class="cs-workbench__sectionLabel">Exploit context</span>
              <ExploitConditionForm
                v-model:exploitCondition="exploitCondition"
                v-model:impactScope="impactScope"
                v-model:businessScenario="businessScenario"
              />
            </div>

            <div class="cs-workbench__section">
              <span class="cs-workbench__sectionLabel">Proof of concept</span>
              <PocUploader v-model="pocAttachments" v-model:content="pocContent" />
            </div>

            <el-collapse v-model="activeCollapse" @change="onCollapseChange" class="cs-workbench__collapse">
              <el-collapse-item title="Standard fix snippet" name="fix">
                <div style="display: flex; flex-direction: column; gap: var(--cs-space-3)">
                  <div>
                    <span class="cs-workbench__sectionLabel" style="margin-bottom: var(--cs-space-1); display: block">Fix description</span>
                    <el-input
                      v-model="fixSuggestion"
                      type="textarea"
                      :rows="2"
                      placeholder="A short, prescriptive fix instruction developers can follow."
                    />
                  </div>
                  <FixSnippetEditorAsync v-if="fixPanelOpen" v-model="fixCodeSnippet" :language="codeLanguage" height="200px" />
                </div>
              </el-collapse-item>
            </el-collapse>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card v-if="showAiPanel" shadow="never" class="cs-workbench__aiCard">
      <div v-if="aiAnalyzing" class="cs-workbench__aiLoading">
        <el-skeleton :rows="3" animated />
        <p style="color: var(--cs-text-tertiary); font-size: 12px; margin-top: 8px">AI analysis in progress…</p>
      </div>
      <AiAuditPanel
        v-else-if="vuln?.aiVerdict"
        :vuln-id="vuln.id"
        :ai-verdict="vuln.aiVerdict as any"
        :ai-confidence="vuln.aiConfidence"
        :ai-explanation="vuln.aiExplanation"
        :ai-generated-patch="vuln.aiGeneratedPatch"
        :original-code="vuln?.codeSnippet"
        :language="detectLanguage(vuln?.filePath)"
        @retry="runAiAnalysis"
      />
      <div v-else class="cs-workbench__aiLoading">
        <p style="color: var(--cs-text-tertiary)">Analysis returned no result</p>
      </div>
    </el-card>

    <el-card shadow="never" class="cs-workbench__cpgCard">
      <template #header>
        <el-space :size="6">
          <el-icon><Connection /></el-icon>
          <span>Call Graph</span>
        </el-space>
      </template>
      <CallGraphView v-if="vuln" :vuln-id="String(vuln.id)" />
    </el-card>

    <el-card shadow="never" class="cs-workbench__historyCard" style="margin-top: var(--cs-space-4)" v-memo="[history, historyLoading]">
      <template #header>
        <el-space :size="6">
          <el-icon><Search /></el-icon>
          <span>Audit history</span>
          <el-tag effect="plain">{{ (history ?? []).length }} actions</el-tag>
        </el-space>
      </template>
      <AuditHistoryTimeline :records="history" :loading="historyLoading" />
    </el-card>
  </div>

  <!-- Loading state -->
  <div v-else-if="!vulnError" class="cs-workbench__loading">
    <div class="cs-workbench__loadingSkeleton">
      <SkeletonCard :lines="4" />
      <div style="height: var(--cs-space-4)" />
      <SkeletonCard :lines="6" />
    </div>
  </div>

  <!-- Error state -->
  <div v-else class="cs-workbench__error">
    <el-empty description="Failed to load finding">
      <template #default>
        <el-button type="primary" @click="retry">
          <el-icon><Refresh /></el-icon> Retry
        </el-button>
      </template>
    </el-empty>
  </div>
</template>

<style scoped>
.cs-workbench__exploitCard {
  background: var(--cs-bg-elevated);
  border: 1px solid var(--cs-border-light);
  border-radius: var(--cs-radius-lg);
  margin-bottom: var(--cs-space-4);
}
.cs-workbench__exploitCard :deep(.el-card__body) {
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
  margin: 0;
  color: var(--cs-text-primary);
  font-size: var(--cs-font-size-sm);
  line-height: var(--cs-line-height-relaxed);
}
.cs-workbench__exploitReasonEmpty {
  font-size: var(--cs-font-size-sm);
  font-style: italic;
  color: var(--cs-text-tertiary);
}
.cs-workbench__codeCard {
  background: var(--cs-bg-elevated);
  border: 1px solid var(--cs-border-light);
  border-radius: var(--cs-radius-lg);
  height: calc(100vh - 240px);
  display: flex;
  flex-direction: column;
}
.cs-workbench__codeCard :deep(.el-card__body) {
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
.cs-workbench__codeFallback {
  margin: 0;
  padding: var(--cs-space-3);
  overflow: auto;
  height: 100%;
  background: #1e1e1e;
  color: #d4d4d4;
  font: 13px/1.5 JetBrains Mono, SF Mono, Menlo, Consolas, monospace;
  white-space: pre;
  tab-size: 2;
  border-radius: var(--cs-radius-sm);
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
  color: var(--cs-text-tertiary);
}
.cs-workbench__lang {
  font-family: var(--cs-font-mono);
  font-size: 11px;
  color: var(--cs-text-tertiary);
}
.cs-workbench__desc {
  font-size: var(--cs-font-size-sm);
  color: var(--cs-text-secondary);
  line-height: var(--cs-line-height-relaxed);
  margin: 0;
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
.cs-workbench__collapse :deep(.el-collapse-item__header) {
  font-weight: 600;
  font-size: var(--cs-font-size-sm);
  color: var(--cs-text-primary);
  border: 1px solid var(--cs-border-light);
  border-radius: var(--cs-radius-md);
  background: var(--cs-bg-sunken);
  padding: 0 var(--cs-space-3);
}
.cs-workbench__collapse :deep(.el-collapse-item__wrap) {
  border: 1px solid var(--cs-border-light);
  border-top: none;
  border-radius: 0 0 var(--cs-radius-md) var(--cs-radius-md);
  background: var(--cs-bg-sunken);
}
.cs-workbench__cpgCard {
  margin-top: var(--cs-space-4);
  background: var(--cs-bg-elevated);
  border: 1px solid var(--cs-border-light);
  border-radius: var(--cs-radius-lg);
}
.cs-workbench__historyCard {
  background: var(--cs-bg-elevated);
  border: 1px solid var(--cs-border-light);
  border-radius: var(--cs-radius-lg);
}
.cs-workbench__aiCard {
  margin-top: var(--cs-space-4);
  background: var(--cs-bg-elevated);
  border: 1px solid var(--cs-border-light);
  border-radius: var(--cs-radius-lg);
}
.cs-workbench__loading {
  padding: var(--cs-space-12);
  text-align: center;
}
.cs-workbench__error {
  padding: var(--cs-space-12);
  text-align: center;
}
</style>
