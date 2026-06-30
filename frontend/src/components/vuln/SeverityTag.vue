<script setup lang="ts">
import { computed } from 'vue'
import type { Severity } from '@/types/vuln'
import { SEVERITY_LABEL } from '@/types/vuln'

interface Props {
  severity: Severity
  size?: 'sm' | 'md'
}

const props = withDefaults(defineProps<Props>(), {
  size: 'md',
})

const styleVars = computed(() => {
  const map: Record<Severity, { color: string; bg: string }> = {
    critical: { color: 'var(--cs-severity-critical)', bg: 'var(--cs-severity-critical-bg)' },
    high: { color: 'var(--cs-severity-high)', bg: 'var(--cs-severity-high-bg)' },
    medium: { color: 'var(--cs-severity-medium)', bg: 'var(--cs-severity-medium-bg)' },
    low: { color: 'var(--cs-severity-low)', bg: 'var(--cs-severity-low-bg)' },
    info: { color: 'var(--cs-severity-info)', bg: 'var(--cs-severity-info-bg)' },
  }
  const s = map[props.severity]
  return {
    color: s.color,
    backgroundColor: s.bg,
  }
})
</script>

<template>
  <span
    class="cs-severity-tag"
    :class="`cs-severity-tag--${size}`"
    :style="styleVars"
  >
    {{ SEVERITY_LABEL[severity] }}
  </span>
</template>

<style scoped>
.cs-severity-tag--sm {
  font-size: 10px;
  padding: 1px 6px;
}
</style>
