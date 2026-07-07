<script setup lang="ts">
import { computed } from 'vue'
import type { SnippetSearchResult } from '@/api/search'
import SeverityTag from '@/components/vuln/SeverityTag.vue'

const props = defineProps<{
  item: SnippetSearchResult
}>()

const languageType: Record<string, string> = {
  java: 'warning',
  go: 'info',
  python: 'primary',
  typescript: '',
  javascript: 'warning',
  php: 'danger',
  csharp: 'success',
}

const severity = computed(() => {
  const s = props.item.severity
  if (s === 'critical' || s === 'high' || s === 'medium' || s === 'low' || s === 'info') return s
  return undefined
})

const lineLabel = computed(() => {
  if (props.item.lineEnd && props.item.lineEnd !== props.item.lineStart) {
    return `L${props.item.lineStart}–L${props.item.lineEnd}`
  }
  if (props.item.lineStart != null) return `L${props.item.lineStart}`
  return ''
})

const exploitTag = computed(() => {
  switch (props.item.exploitability) {
    case 'exploitable': return { type: 'danger' as const, label: 'Exploitable' }
    case 'not_exploitable': return { type: 'info' as const, label: 'Not exploitable' }
    case 'potentially_exploitable': return { type: 'warning' as const, label: 'Potentially' }
    default: return null
  }
})
</script>

<template>
  <div class="cs-snippet-result">
    <div class="cs-snippet-result__header">
      <el-tag :type="(languageType[item.language] as any) ?? ''" size="small">
        {{ item.language }}
      </el-tag>
      <SeverityTag v-if="severity" :severity="severity" size="sm" />
      <el-tag v-if="item.cwe" type="danger" size="small">{{ item.cwe }}</el-tag>
      <el-tag v-if="exploitTag" :type="exploitTag.type" size="small" effect="plain">
        {{ exploitTag.label }}
      </el-tag>
      <span class="cs-snippet-result__line">{{ lineLabel }}</span>
    </div>
    <div v-if="item.title" class="cs-snippet-result__title">
      {{ item.title }}
    </div>
    <div class="cs-snippet-result__path">
      <code>{{ item.filePath }}</code>
    </div>
    <div v-if="item.codeSnippet" class="cs-snippet-result__snippet">
      <code v-html="item.codeSnippet"></code>
    </div>
    <div class="cs-snippet-result__meta">
      <span>Project: {{ item.projectId }}</span>
      <span>Indexed: {{ item.indexedAt?.substring(0, 10) }}</span>
    </div>
  </div>
</template>

<style scoped>
.cs-snippet-result {
  padding: 12px 16px;
  border: 1px solid var(--cs-border);
  border-radius: var(--cs-radius-md);
  transition: border-color var(--cs-duration-fast);
}
.cs-snippet-result:hover {
  border-color: var(--cs-color-primary);
}
.cs-snippet-result__header {
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.cs-snippet-result__title {
  font-size: var(--cs-font-size-sm);
  font-weight: 600;
  color: var(--cs-text-primary);
  margin-bottom: 4px;
  line-height: 1.4;
}
.cs-snippet-result__line {
  font-size: var(--cs-font-size-xs);
  color: var(--cs-text-tertiary);
}
.cs-snippet-result__path {
  margin-bottom: 6px;
}
.cs-snippet-result__path code {
  font-family: 'SF Mono', 'Fira Code', monospace;
  font-size: 13px;
  color: var(--cs-text-primary);
  word-break: break-all;
}
.cs-snippet-result__snippet {
  margin: 8px 0;
  padding: 8px 12px;
  background: var(--cs-bg-secondary);
  border-radius: var(--cs-radius-sm);
  overflow-x: auto;
}
.cs-snippet-result__snippet code {
  font-family: 'SF Mono', 'Fira Code', monospace;
  font-size: 12px;
  line-height: 1.5;
  color: var(--cs-text-primary);
  white-space: pre-wrap;
  word-break: break-all;
}
.cs-snippet-result__snippet :deep(em) {
  font-style: normal;
  background: #fff3b0;
  padding: 0 2px;
  border-radius: 2px;
}
.cs-snippet-result__meta {
  display: flex;
  gap: 16px;
  font-size: var(--cs-font-size-xs);
  color: var(--cs-text-tertiary);
}
</style>
