<script setup lang="ts">
import { computed } from 'vue'
import { useWorkbenchLayout } from '../composables/useWorkbenchLayout'
import VulnListSidebar from './VulnListSidebar.vue'
import CodeMainPanel from './CodeMainPanel.vue'
import ActionRightPanel from './ActionRightPanel.vue'

const CONTEXT_LINES = 5

const props = withDefaults(defineProps<{
  vulns?: Array<{
    id: string
    title: string
    severity: string
    filePath: string
    codeSnippet?: string
    language?: string
    lineStart?: number
    lineEnd?: number
    status?: string
    aiVerdict?: string
    aiConfidence?: number
    aiExplanation?: string
    aiGeneratedPatch?: string
  }>
  selectedVulnId?: string
}>(), { vulns: () => [] })

const emit = defineEmits<{
  selectVuln: [id: string]
}>()

const layout = useWorkbenchLayout()

const selectedVuln = computed(() =>
  props.vulns.find((v) => v.id === props.selectedVulnId)
)

const snippetStartLine = computed(() => {
  const v = selectedVuln.value
  if (!v?.lineStart) return 1
  return Math.max(1, v.lineStart - CONTEXT_LINES)
})

const highlightLines = computed<[number, number] | undefined>(() => {
  const v = selectedVuln.value
  if (!v?.lineStart) return undefined
  return [v.lineStart, v.lineEnd ?? v.lineStart]
})
</script>

<template>
  <div class="cs-workbench">
    <div
      class="cs-workbench-left"
      :style="{ width: layout.leftCollapsed ? '40px' : layout.leftPanelWidth + 'px' }"
    >
      <VulnListSidebar
        v-if="!layout.leftCollapsed"
        :items="vulns.map(v => ({ id: v.id, title: v.title, severity: v.severity, filePath: v.filePath, status: v.status }))"
        :selected-id="selectedVulnId"
        @select="emit('selectVuln', $event)"
      />
      <button class="cs-workbench-toggle" @click="layout.toggleLeft()">
        {{ layout.leftCollapsed ? '>' : '<' }}
      </button>
    </div>

    <div class="cs-workbench-center">
      <CodeMainPanel
        v-if="selectedVuln"
        :code="selectedVuln.codeSnippet"
        :language="selectedVuln.language"
        :start-line="snippetStartLine"
        :highlight-lines="highlightLines"
      />
      <el-empty v-else description="Select a vulnerability to review" />
    </div>

    <div
      class="cs-workbench-right"
      :style="{ width: layout.rightCollapsed ? '40px' : layout.rightPanelWidth + 'px' }"
    >
      <ActionRightPanel
        v-if="selectedVuln && !layout.rightCollapsed"
        :vuln-id="selectedVuln.id"
        :ai-verdict="(selectedVuln.aiVerdict as any)"
        :ai-confidence="selectedVuln.aiConfidence"
        :ai-explanation="selectedVuln.aiExplanation"
        :ai-generated-patch="selectedVuln.aiGeneratedPatch"
        :original-code="selectedVuln.codeSnippet"
        :language="selectedVuln.language"
      />
      <button class="cs-workbench-toggle" @click="layout.toggleRight()">
        {{ layout.rightCollapsed ? '<' : '>' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.cs-workbench {
  display: flex;
  height: 100%;
  overflow: hidden;
}

.cs-workbench-left {
  display: flex;
  border-right: 1px solid var(--cs-border);
  background: var(--cs-bg-elevated);
  transition: width 0.2s ease;
  flex-shrink: 0;
  overflow: hidden;
}

.cs-workbench-center {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.cs-workbench-right {
  display: flex;
  border-left: 1px solid var(--cs-border);
  background: var(--cs-bg-elevated);
  transition: width 0.2s ease;
  flex-shrink: 0;
  overflow: hidden;
}

.cs-workbench-toggle {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 20px;
  height: 40px;
  border: 1px solid var(--cs-border);
  background: var(--cs-bg-elevated);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--cs-text-tertiary);
  z-index: 5;
}

.cs-workbench-left .cs-workbench-toggle {
  right: -10px;
  border-radius: 0 4px 4px 0;
}

.cs-workbench-right .cs-workbench-toggle {
  left: -10px;
  border-radius: 4px 0 0 4px;
}
</style>
