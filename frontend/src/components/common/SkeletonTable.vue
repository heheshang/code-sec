<script setup lang="ts">
interface Props {
  rows?: number
  columns?: number
}

withDefaults(defineProps<Props>(), {
  rows: 5,
  columns: 4,
})
</script>

<template>
  <div class="cs-skeleton-table">
    <div class="cs-skeleton-table__header">
      <div
        v-for="c in columns"
        :key="c"
        class="cs-skeleton-table__cell cs-skeleton-table__cell--header"
      >
        <div class="cs-skeleton-box" style="width: 60%; height: 14px" />
      </div>
    </div>
    <div
      v-for="r in rows"
      :key="r"
      class="cs-skeleton-table__row"
    >
      <div
        v-for="c in columns"
        :key="c"
        class="cs-skeleton-table__cell"
      >
        <div
          class="cs-skeleton-box"
          :style="{ width: (40 + Math.random() * 40) + '%', height: '12px' }"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.cs-skeleton-table {
  border: 1px solid var(--cs-border-subtle);
  border-radius: var(--cs-radius-md);
  overflow: hidden;
}
.cs-skeleton-table__header {
  display: flex;
  background: var(--cs-bg-subtle);
  border-bottom: 1px solid var(--cs-border-subtle);
}
.cs-skeleton-table__row {
  display: flex;
  border-bottom: 1px solid var(--cs-border-subtle);
}
.cs-skeleton-table__row:last-child {
  border-bottom: none;
}
.cs-skeleton-table__cell {
  flex: 1;
  padding: var(--cs-space-3) var(--cs-space-4);
}
.cs-skeleton-table__cell--header {
  padding: var(--cs-space-3) var(--cs-space-4);
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
