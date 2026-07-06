<script setup lang="ts">
import { computed } from 'vue'
import { WarningFilled } from '@element-plus/icons-vue'
import AiConfidenceBadge from './AiConfidenceBadge.vue'
import AiVerdictTimeline from './AiVerdictTimeline.vue'
import AiFixSuggestion from './AiFixSuggestion.vue'
import LogicVulnSection from './LogicVulnSection.vue'
import type { AiVerdict, PipelineStageInfo, PocPatchStatus, LogicVulnResult } from '@/types/ai-audit'
import { AI_VERDICT_LABEL, AI_VERDICT_COLOR } from '@/types/ai-audit'

const props = defineProps<{
  vulnId: string
  aiVerdict?: AiVerdict
  aiConfidence?: number
  aiExplanation?: string
  aiGeneratedPatch?: string
  originalCode?: string
  language?: string
  pocStatus?: PocPatchStatus
  patchStatus?: PocPatchStatus
  logicVulnResults?: LogicVulnResult[]
  logicVulnLoading?: boolean
}>()

const emit = defineEmits<{
  retry: []
}>()

const hasError = computed(() => {
  return props.aiExplanation?.startsWith('Analysis failed:') ?? false
})

const uiMessage = computed(() => {
  if (!props.aiExplanation) return ''
  const msg = props.aiExplanation
  if (msg.includes('429') || msg.includes('rate limit') || msg.includes('usage limit') || msg.includes('quota')) {
    return 'AI analysis is temporarily unavailable due to upstream service rate limiting. Please wait a moment and try again.'
  }
  if (msg.includes('timed out') || msg.includes('timeout') || msg.includes('TimeoutException')) {
    return 'AI analysis request timed out. The service may be under heavy load. Please try again.'
  }
  if (msg.includes('auth') || msg.includes('unauthorized') || msg.includes('401')) {
    return 'AI service authentication failed. Please contact the administrator.'
  }
  return 'AI analysis encountered an error. Please try again.'
})

const demoStages: PipelineStageInfo[] = [
  { name: 'false_positive_filter', success: true },
  { name: 'vuln_analysis', success: true },
  { name: 'logic_vuln_mining', success: !!(props.logicVulnResults && props.logicVulnResults.length > 0) },
  { name: 'poc_generation', success: props.pocStatus === 'PASS' },
  { name: 'patch_generation', success: props.patchStatus === 'PASS' },
]

const verdictLabel = computed(() =>
  props.aiVerdict ? AI_VERDICT_LABEL[props.aiVerdict] : 'Pending'
)

const verdictColor = computed(() =>
  props.aiVerdict ? AI_VERDICT_COLOR[props.aiVerdict] : 'var(--cs-text-tertiary)'
)
</script>

<template>
  <div class="cs-ai-panel">
    <!-- Error state -->
    <div v-if="hasError" class="cs-ai-error">
      <div class="cs-ai-error__icon">
        <el-icon :size="36" color="var(--cs-status-retest)">
          <WarningFilled />
        </el-icon>
      </div>
      <h4 class="cs-ai-error__title">Analysis Failed</h4>
      <p class="cs-ai-error__message">{{ uiMessage }}</p>
      <el-button type="primary" size="small" @click="emit('retry')">
        Retry Analysis
      </el-button>
    </div>

    <!-- Normal result -->
    <template v-else>
      <div class="cs-ai-panel-header">
        <h4>AI Audit Result</h4>
        <el-tag
          v-if="aiVerdict"
          :color="verdictColor"
          effect="dark"
          size="small"
        >
          {{ verdictLabel }}
        </el-tag>
        <el-tag v-else type="info" size="small">Pending</el-tag>
      </div>

      <div class="cs-ai-panel-section">
        <div class="cs-ai-panel-row">
          <span class="cs-ai-panel-label">Confidence</span>
          <AiConfidenceBadge
            v-if="aiConfidence && aiConfidence > 0"
            :confidence="aiConfidence"
            size="small"
          />
          <span v-else class="cs-ai-panel-na">N/A</span>
        </div>

        <div class="cs-ai-panel-row">
          <span class="cs-ai-panel-label">Explanation</span>
          <p v-if="aiExplanation" class="cs-ai-panel-text">{{ aiExplanation }}</p>
          <span v-else class="cs-ai-panel-na">No explanation available</span>
        </div>
      </div>

      <div class="cs-ai-panel-section">
        <div class="cs-ai-panel-label">Pipeline</div>
        <AiVerdictTimeline :stages="demoStages" />
      </div>

      <div class="cs-ai-panel-section">
        <AiFixSuggestion
          :original-code="originalCode"
          :fixed-code="aiGeneratedPatch"
          :language="language"
          :poc-status="pocStatus"
          :patch-status="patchStatus"
        />
      </div>

      <div v-if="logicVulnResults" class="cs-ai-panel-section">
        <LogicVulnSection :results="logicVulnResults" :loading="logicVulnLoading" />
      </div>
    </template>
  </div>
</template>

<style scoped>
.cs-ai-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.cs-ai-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.cs-ai-panel-header h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
}

.cs-ai-panel-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.cs-ai-panel-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.cs-ai-panel-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--cs-text-tertiary);
  text-transform: uppercase;
  min-width: 90px;
  flex-shrink: 0;
  padding-top: 2px;
}

.cs-ai-panel-text {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--cs-text-primary);
}

.cs-ai-panel-na {
  font-size: 12px;
  color: var(--cs-text-disabled);
  font-style: italic;
}

.cs-ai-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 32px 16px;
  text-align: center;
}

.cs-ai-error__icon {
  margin-bottom: 4px;
}

.cs-ai-error__title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--cs-text-primary);
}

.cs-ai-error__message {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--cs-text-secondary);
  max-width: 400px;
}
</style>
