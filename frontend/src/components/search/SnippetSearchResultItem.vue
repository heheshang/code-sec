<script setup lang="ts">
import type { SnippetSearchResult } from '@/api/search'

defineProps<{
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
</script>

<template>
  <div class="cs-snippet-result">
    <div class="cs-snippet-result__header">
      <el-tag :type="(languageType[item.language] as any) ?? ''" size="small">
        {{ item.language }}
      </el-tag>
      <span v-if="item.lineStart != null" class="cs-snippet-result__line">Line {{ item.lineStart }}</span>
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
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
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
