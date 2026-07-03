<script setup lang="ts">
defineProps<{
  percent: number
  phase: string
}>()
</script>

<template>
  <div class="cs-scan-progress">
    <div class="cs-scan-progress-info">
      <span class="cs-scan-progress-phase">{{ phase }}</span>
      <span class="cs-scan-progress-pct">{{ percent }}%</span>
    </div>
    <el-progress
      :percentage="percent"
      :stroke-width="8"
      :show-text="false"
      color="var(--cs-color-primary)"
    />
  </div>
</template>

<style scoped>
.cs-scan-progress {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.cs-scan-progress-info {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
}

.cs-scan-progress-phase {
  color: var(--cs-text-secondary);
}

.cs-scan-progress-pct {
  font-family: var(--cs-font-mono);
  color: var(--cs-text-primary);
  font-weight: 600;
}
</style>
