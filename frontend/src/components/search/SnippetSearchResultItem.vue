<script setup lang="ts">
import { Tag } from 'ant-design-vue'
import type { SnippetSearchResult } from '@/api/search'

defineProps<{
  item: SnippetSearchResult
}>()

const languageColors: Record<string, string> = {
  java: 'orange',
  go: 'cyan',
  python: 'blue',
  typescript: 'geekblue',
  javascript: 'gold',
  php: 'purple',
  csharp: 'green',
}
</script>

<template>
  <div class="cs-snippet-result">
    <div class="cs-snippet-result__header">
      <Tag :color="languageColors[item.language] ?? 'default'">
        {{ item.language }}
      </Tag>
    </div>
    <div class="cs-snippet-result__path">
      <code>{{ item.filePath }}</code>
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
.cs-snippet-result__meta {
  display: flex;
  gap: 16px;
  font-size: var(--cs-font-size-xs);
  color: var(--cs-text-tertiary);
}
</style>
