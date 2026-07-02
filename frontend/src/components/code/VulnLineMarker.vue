<script setup lang="ts">
/**
 * VulnLineMarker
 * Standalone legend / status chip for the vulnerability under review.
 * Pairs with CodeViewer for a full context block.
 */
import { computed } from 'vue'
import { Document } from '@element-plus/icons-vue'
import type { Vuln } from '@/types/vuln'
import SeverityTag from '@/components/vuln/SeverityTag.vue'

interface Props {
  vuln: Vuln
}

const props = defineProps<Props>()

const cve = computed<string | null>(() => props.vuln.cve)
</script>

<template>
  <div class="cs-vuln-line-marker">
    <div class="cs-vuln-line-marker__head">
      <el-icon class="cs-vuln-line-marker__icon"><Document /></el-icon>
      <span class="cs-vuln-line-marker__path">
        {{ vuln.filePath }}<span class="cs-vuln-line-marker__lines">:{{ vuln.lineStart }}–{{ vuln.lineEnd }}</span>
      </span>
    </div>
    <el-space :size="6" wrap>
      <SeverityTag :severity="vuln.severity" size="sm" />
      <el-tag v-if="cve !== null" type="danger">{{ cve }}</el-tag>
      <el-tag>{{ vuln.cwe }}</el-tag>
      <el-tag type="info">{{ vuln.engine }}</el-tag>
    </el-space>
  </div>
</template>

<style scoped>
.cs-vuln-line-marker {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cs-space-3);
  padding: var(--cs-space-2) var(--cs-space-3);
  background: var(--cs-bg-elevated);
  border: 1px solid var(--cs-border-light);
  border-radius: var(--cs-radius-md);
  flex-wrap: wrap;
}
.cs-vuln-line-marker__head {
  display: flex;
  align-items: center;
  gap: var(--cs-space-2);
  min-width: 0;
}
.cs-vuln-line-marker__icon {
  color: var(--cs-color-primary);
  font-size: 14px;
}
.cs-vuln-line-marker__path {
  font-family: var(--cs-font-mono);
  font-size: var(--cs-font-size-sm);
  color: var(--cs-text-primary);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cs-vuln-line-marker__lines {
  color: var(--cs-severity-critical);
  font-weight: 600;
}
</style>
