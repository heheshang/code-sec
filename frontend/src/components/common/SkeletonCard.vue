<script setup lang="ts">
interface Props {
  lines?: number
}

withDefaults(defineProps<Props>(), {
  lines: 3,
})
</script>

<template>
  <div class="cs-skeleton-card">
    <div class="cs-skeleton-card__header">
      <div class="cs-skeleton-box" style="width: 40%; height: 16px" />
    </div>
    <div class="cs-skeleton-card__body">
      <div
        v-for="i in lines"
        :key="i"
        class="cs-skeleton-box"
        :style="{ width: (50 + Math.random() * 40) + '%', height: '12px' }"
      />
    </div>
  </div>
</template>

<style scoped>
.cs-skeleton-card {
  border: 1px solid var(--cs-border-subtle);
  border-radius: var(--cs-radius-md);
  padding: var(--cs-space-5);
  background: var(--cs-bg-primary);
}
.cs-skeleton-card__header {
  margin-bottom: var(--cs-space-4);
}
.cs-skeleton-card__body {
  display: flex;
  flex-direction: column;
  gap: var(--cs-space-3);
}
.cs-skeleton-box {
  background: var(--cs-bg-emphasis);
  border-radius: 4px;
  animation: cs-pulse 1.5s ease-in-out infinite;
}
@keyframes cs-pulse {
  0%, 100% { opacity: 0.4; }
  50% { opacity: 0.8; }
}
</style>
