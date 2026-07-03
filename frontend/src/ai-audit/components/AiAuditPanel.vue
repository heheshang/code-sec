<script setup lang="ts">
import { computed } from 'vue'
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
</style>
