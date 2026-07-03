<script setup lang="ts">
import { ref } from 'vue'
import type { PocPatchStatus } from '@/types/ai-audit'
import { POC_PATCH_STATUS_LABEL, POC_PATCH_STATUS_TYPE } from '@/types/ai-audit'

const props = defineProps<{
  originalCode?: string
  fixedCode?: string
  language?: string
  pocStatus?: PocPatchStatus
  patchStatus?: PocPatchStatus
}>()

const collapsed = ref(true)
</script>

<template>
  <div class="cs-ai-fix">
    <div class="cs-ai-fix-header" @click="collapsed = !collapsed">
      <span class="cs-ai-fix-title">
        <el-icon><component :is="collapsed ? 'ArrowRight' : 'ArrowDown'" /></el-icon>
        AI Fix Suggestion
      </span>
      <el-space :size="6">
        <el-tooltip v-if="pocStatus" content="POC verification status" placement="top">
          <el-tag :type="POC_PATCH_STATUS_TYPE[pocStatus]" size="small">
            POC {{ POC_PATCH_STATUS_LABEL[pocStatus] }}
          </el-tag>
        </el-tooltip>
        <el-tooltip v-if="patchStatus" content="Patch compilation status" placement="top">
          <el-tag :type="POC_PATCH_STATUS_TYPE[patchStatus]" size="small">
            Patch {{ POC_PATCH_STATUS_LABEL[patchStatus] }}
          </el-tag>
        </el-tooltip>
        <el-tag v-if="fixedCode" type="success" size="small">Available</el-tag>
        <el-tag v-else type="info" size="small">No fix generated</el-tag>
      </el-space>
    </div>

    <div v-if="!collapsed && fixedCode" class="cs-ai-fix-body">
      <div class="cs-ai-fix-diff">
        <div class="cs-ai-fix-panel">
          <div class="cs-ai-fix-panel-label">Original</div>
          <pre class="cs-ai-fix-code cs-ai-fix-original">{{ originalCode || 'N/A' }}</pre>
        </div>
        <div class="cs-ai-fix-arrow">
          <el-icon size="20"><Right /></el-icon>
        </div>
        <div class="cs-ai-fix-panel">
          <div class="cs-ai-fix-panel-label">Fixed</div>
          <pre class="cs-ai-fix-code cs-ai-fix-fixed">{{ fixedCode }}</pre>
        </div>
      </div>

      <div class="cs-ai-fix-actions">
        <el-button size="small" type="primary">
          <el-icon><CopyDocument /></el-icon>
          Copy fix
        </el-button>
        <el-button size="small">
          <el-icon><Upload /></el-icon>
          Apply fix
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cs-ai-fix {
  border: 1px solid var(--cs-border);
  border-radius: var(--cs-radius-md);
  overflow: hidden;
}

.cs-ai-fix-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  cursor: pointer;
  user-select: none;
  background: var(--cs-bg-sunken);
}

.cs-ai-fix-header:hover {
  background: var(--cs-bg-hover);
}

.cs-ai-fix-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
}

.cs-ai-fix-body {
  padding: 12px;
}

.cs-ai-fix-diff {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 8px;
  align-items: stretch;
}

.cs-ai-fix-panel-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--cs-text-tertiary);
  text-transform: uppercase;
  margin-bottom: 4px;
}

.cs-ai-fix-code {
  font-family: var(--cs-font-mono);
  font-size: 12px;
  padding: 8px;
  border-radius: var(--cs-radius-sm);
  overflow-x: auto;
  white-space: pre;
  max-height: 200px;
  overflow-y: auto;
}

.cs-ai-fix-original {
  background: rgba(207, 19, 34, 0.06);
  border: 1px solid rgba(207, 19, 34, 0.15);
}

.cs-ai-fix-fixed {
  background: rgba(0, 185, 107, 0.06);
  border: 1px solid rgba(0, 185, 107, 0.15);
}

.cs-ai-fix-arrow {
  display: flex;
  align-items: center;
  color: var(--cs-text-tertiary);
}

.cs-ai-fix-actions {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}
</style>
