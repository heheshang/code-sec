<script setup lang="ts">
import type { VNode } from 'vue'

interface Props {
  title?: string
  description?: string
  imageSize?: number
}

withDefaults(defineProps<Props>(), {
  title: 'Nothing here yet',
  description: '',
  imageSize: 120,
})

defineSlots<{
  default?: () => VNode[]
  action?: () => VNode[]
}>()
</script>

<template>
  <div class="cs-empty">
    <div class="cs-empty__mark">
      <svg :width="imageSize" :height="imageSize" viewBox="0 0 120 120" fill="none">
        <circle cx="60" cy="60" r="48" stroke="var(--cs-border-strong)" stroke-width="2" stroke-dasharray="4 6" />
        <path d="M40 60h40M60 40v40" stroke="var(--cs-text-tertiary)" stroke-width="2" stroke-linecap="round" />
      </svg>
    </div>
    <div class="cs-empty__body">
      <div class="cs-empty__title">{{ title }}</div>
      <div v-if="description" class="cs-empty__desc">{{ description }}</div>
      <div v-if="$slots.action" class="cs-empty__action">
        <slot name="action" />
      </div>
    </div>
    <slot />
  </div>
</template>

<style scoped>
.cs-empty {
  padding: var(--cs-space-12) var(--cs-space-6);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.cs-empty__mark {
  display: flex;
  align-items: center;
  justify-content: center;
}
.cs-empty__body {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--cs-space-1);
  margin-top: var(--cs-space-2);
}
.cs-empty__title {
  font-size: var(--cs-font-size-md);
  font-weight: 600;
  color: var(--cs-text-primary);
}
.cs-empty__desc {
  font-size: var(--cs-font-size-sm);
  color: var(--cs-text-tertiary);
  max-width: 360px;
  text-align: center;
}
.cs-empty__action {
  margin-top: var(--cs-space-3);
}
</style>
