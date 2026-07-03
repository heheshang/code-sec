<script setup lang="ts">
import AiAuditPanel from '@/ai-audit/components/AiAuditPanel.vue'
import TaintFlowView from '@/graph/components/TaintFlowView.vue'
import type { AiVerdict } from '@/types/ai-audit'

defineProps<{
  vulnId: string
  aiVerdict?: AiVerdict
  aiConfidence?: number
  aiExplanation?: string
  aiGeneratedPatch?: string
  originalCode?: string
  language?: string
}>()
</script>

<template>
  <div class="cs-action-panel">
    <el-tabs class="cs-action-tabs" tab-position="top">
      <el-tab-pane label="AI Audit">
        <AiAuditPanel
          :vuln-id="vulnId"
          :ai-verdict="aiVerdict"
          :ai-confidence="aiConfidence"
          :ai-explanation="aiExplanation"
          :ai-generated-patch="aiGeneratedPatch"
          :original-code="originalCode"
          :language="language"
        />
      </el-tab-pane>

      <el-tab-pane label="Taint Flow">
        <TaintFlowView :nodes="[]" />
      </el-tab-pane>

      <el-tab-pane label="Actions">
        <div class="cs-action-buttons">
          <el-button type="primary" style="width: 100%">
            <el-icon><CircleCheck /></el-icon>
            Confirm
          </el-button>
          <el-button type="warning" style="width: 100%">
            <el-icon><CircleClose /></el-icon>
            False Positive
          </el-button>
          <el-button style="width: 100%">
            <el-icon><Edit /></el-icon>
            Request Retest
          </el-button>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.cs-action-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.cs-action-tabs {
  flex: 1;
  padding: 0 12px;
}

.cs-action-buttons {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 0;
}
</style>
