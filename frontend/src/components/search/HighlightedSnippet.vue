<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  html: string
  maxLength?: number
}>()

/**
 * Renders PostgreSQL FTS highlight HTML safely.
 * Strips any script/event-handler attributes to prevent XSS.
 */
const safeHtml = computed(() => {
  let html = props.html || ''
  // Strip dangerous attributes
  html = html.replace(/on\w+\s*=\s*"[^"]*"/gi, '')
  html = html.replace(/<script[^>]*>.*?<\/script>/gi, '')
  // Truncate if needed
  if (props.maxLength && html.length > props.maxLength) {
    html = html.substring(0, props.maxLength) + '…'
  }
  return html
})
</script>

<template>
  <span class="cs-highlight" v-html="safeHtml" />
</template>

<style scoped>
.cs-highlight :deep(em) {
  font-style: normal;
  font-weight: 600;
  color: var(--cs-color-primary);
  background: rgba(91, 71, 224, 0.1);
  border-radius: 2px;
  padding: 0 2px;
}
</style>
