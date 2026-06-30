<script setup lang="ts">
/**
 * VulnLineMarker
 * Standalone legend / status chip for the vulnerability under review.
 * Pairs with CodeViewer for a full context block.
 */
import { computed } from 'vue'
import { Tag, Space, Typography } from 'ant-design-vue'
import { CodeOutlined } from '@ant-design/icons-vue'
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
      <CodeOutlined class="cs-vuln-line-marker__icon" />
      <Typography.Text class="cs-vuln-line-marker__path">
        {{ vuln.filePath }}<span class="cs-vuln-line-marker__lines">:{{ vuln.lineStart }}–{{ vuln.lineEnd }}</span>
      </Typography.Text>
    </div>
    <Space :size="6" wrap>
      <SeverityTag :severity="vuln.severity" size="sm" />
      <Tag v-if="cve !== null" color="red" bordered>{{ cve }}</Tag>
      <Tag bordered>{{ vuln.cwe }}</Tag>
      <Tag bordered color="purple">{{ vuln.engine }}</Tag>
    </Space>
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
