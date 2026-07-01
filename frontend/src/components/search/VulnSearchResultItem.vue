<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Tag } from 'ant-design-vue'
import type { VulnSearchResult } from '@/api/search'
import { SEVERITY_LABEL } from '@/types/vuln'
import SeverityTag from '@/components/vuln/SeverityTag.vue'
import HighlightedSnippet from './HighlightedSnippet.vue'

const props = defineProps<{
  item: VulnSearchResult
  highlightFields?: Record<string, string[]>
}>()

const router = useRouter()

const titleHtml = computed(() => {
  const key = `${props.item.id}:title`
  if (props.highlightFields?.[key]?.length) {
    return props.highlightFields[key][0]
  }
  return props.item.title
})

const descHtml = computed(() => {
  const key = `${props.item.id}:description`
  if (props.highlightFields?.[key]?.length) {
    return props.highlightFields[key].join(' … ')
  }
  // Show first 200 chars of description
  return props.item.description?.substring(0, 200) ?? ''
})

const codeHtml = computed(() => {
  const key = `${props.item.id}:code_snippet`
  if (props.highlightFields?.[key]?.length) {
    return props.highlightFields[key][0]
  }
  return props.item.codeSnippet?.substring(0, 300) ?? ''
})

function goToDetail(): void {
  router.push({ name: 'workbench', params: { vulnId: props.item.id } })
}
</script>

<template>
  <div class="cs-vuln-result" @click="goToDetail">
    <div class="cs-vuln-result__header">
      <SeverityTag :severity="item.severity" />
      <span class="cs-vuln-result__cwe">{{ item.cwe }}</span>
      <Tag v-if="item.exploitability === 'EXPLOITABLE'" color="error" class="cs-vuln-result__exploit">
        EXPLOITABLE
      </Tag>
    </div>
    <h4 class="cs-vuln-result__title">
      <HighlightedSnippet :html="titleHtml ?? ''" :max-length="200" />
    </h4>
    <p v-if="descHtml" class="cs-vuln-result__desc">
      <HighlightedSnippet :html="descHtml" :max-length="300" />
    </p>
    <div class="cs-vuln-result__meta">
      <span class="cs-vuln-result__file">{{ item.filePath }}</span>
      <span class="cs-vuln-result__engine">{{ item.engine }}</span>
      <span class="cs-vuln-result__date">{{ item.discoveredAt?.substring(0, 10) }}</span>
    </div>
    <div v-if="codeHtml" class="cs-vuln-result__code">
      <HighlightedSnippet :html="codeHtml" :max-length="400" />
    </div>
  </div>
</template>

<style scoped>
.cs-vuln-result {
  padding: 16px;
  border: 1px solid var(--cs-border);
  border-radius: var(--cs-radius-md);
  cursor: pointer;
  transition: border-color var(--cs-duration-fast), box-shadow var(--cs-duration-fast);
}
.cs-vuln-result:hover {
  border-color: var(--cs-color-primary);
  box-shadow: 0 2px 8px rgba(91, 71, 224, 0.08);
}
.cs-vuln-result__header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.cs-vuln-result__cwe {
  font-size: var(--cs-font-size-xs);
  color: var(--cs-text-tertiary);
  font-family: monospace;
}
.cs-vuln-result__exploit {
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
}
.cs-vuln-result__title {
  margin: 0 0 6px 0;
  font-size: var(--cs-font-size-md);
  font-weight: 600;
  color: var(--cs-text-primary);
  line-height: 1.4;
}
.cs-vuln-result__desc {
  margin: 0 0 8px 0;
  font-size: var(--cs-font-size-sm);
  color: var(--cs-text-secondary);
  line-height: 1.5;
}
.cs-vuln-result__meta {
  display: flex;
  gap: 16px;
  font-size: var(--cs-font-size-xs);
  color: var(--cs-text-tertiary);
  font-family: monospace;
}
.cs-vuln-result__code {
  margin-top: 10px;
  padding: 10px 12px;
  background: var(--cs-bg-base);
  border-radius: 4px;
  font-family: 'SF Mono', 'Fira Code', monospace;
  font-size: 12px;
  line-height: 1.5;
  overflow-x: auto;
}
</style>
