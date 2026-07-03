<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  confidence: number
  size?: 'small' | 'default' | 'large'
}>(), { size: 'default' })

const color = computed(() => {
  if (props.confidence >= 0.8) return 'var(--cs-color-accent)'
  if (props.confidence >= 0.5) return 'var(--cs-status-retest)'
  return 'var(--cs-severity-critical)'
})

const bgColor = computed(() => {
  if (props.confidence >= 0.8) return 'rgba(0, 185, 107, 0.1)'
  if (props.confidence >= 0.5) return 'rgba(250, 173, 20, 0.1)'
  return 'rgba(207, 19, 34, 0.1)'
})

const label = computed(() => `${Math.round(props.confidence * 100)}%`)
const sizePx = computed(() => ({ small: 28, default: 36, large: 44 }[props.size]))
</script>

<template>
  <span
    class="cs-ai-confidence"
    :style="{
      '--badge-color': color,
      '--badge-bg': bgColor,
      '--badge-size': sizePx + 'px',
    }"
  >
    {{ label }}
  </span>
</template>

<style scoped>
.cs-ai-confidence {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: var(--badge-size);
  height: var(--badge-size);
  border-radius: 50%;
  background: var(--badge-bg);
  color: var(--badge-color);
  font-size: 11px;
  font-weight: 700;
  font-family: var(--cs-font-mono);
  border: 2px solid var(--badge-color);
}
</style>
