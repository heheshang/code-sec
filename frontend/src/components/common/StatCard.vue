<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  label: string
  value: number | string
  hint?: string
  trend?: number
  trendInverse?: boolean
  accent?: 'primary' | 'danger' | 'warning' | 'success' | 'neutral'
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  hint: '',
  trend: undefined,
  trendInverse: false,
  accent: 'primary',
  loading: false,
})

const trendClass = computed(() => {
  if (props.trend === undefined) return ''
  if (props.trend === 0) return 'is-neutral'
  const positive = props.trend > 0
  const good = props.trendInverse ? !positive : positive
  return good ? 'is-up' : 'is-down'
})

const trendText = computed(() => {
  if (props.trend === undefined) return ''
  const arrow = props.trend > 0 ? '\u25B2' : props.trend < 0 ? '\u25BC' : '\u2014'
  return `${arrow} ${Math.abs(props.trend).toFixed(1)}%`
})
</script>

<template>
  <div class="cs-stat-card" :class="`cs-stat-card--${accent}`">
    <div class="cs-stat-card__label">{{ label }}</div>
    <el-skeleton v-if="loading" :loading="loading" :paragraph="false">
      <template #default>
        <div class="cs-stat-card__value">{{ value }}</div>
      </template>
    </el-skeleton>
    <div v-else class="cs-stat-card__value">{{ value }}</div>
    <div v-if="hint || trend !== undefined" class="cs-stat-card__meta">
      <span v-if="trend !== undefined" class="cs-stat-card__trend" :class="trendClass">
        {{ trendText }}
      </span>
      <span v-if="hint" class="cs-stat-card__hint">{{ hint }}</span>
    </div>
  </div>
</template>

<style scoped>
.cs-stat-card {
  background: var(--cs-bg-elevated);
  border: 1px solid var(--cs-border-light);
  border-radius: var(--cs-radius-lg);
  padding: var(--cs-space-4) var(--cs-space-5);
  display: flex;
  flex-direction: column;
  gap: var(--cs-space-1);
  position: relative;
  overflow: hidden;
  transition: box-shadow var(--cs-duration-base) var(--cs-ease-out),
    border-color var(--cs-duration-base) var(--cs-ease-out);
}
.cs-stat-card:hover {
  box-shadow: var(--cs-shadow-2);
  border-color: var(--cs-border-strong);
}
.cs-stat-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--cs-color-primary);
}
.cs-stat-card--danger::before { background: var(--cs-severity-critical); }
.cs-stat-card--warning::before { background: var(--cs-severity-medium); }
.cs-stat-card--success::before { background: var(--cs-color-accent); }
.cs-stat-card--neutral::before { background: var(--cs-text-tertiary); }
.cs-stat-card--primary::before { background: var(--cs-color-primary); }

.cs-stat-card__label {
  font-size: var(--cs-font-size-sm);
  color: var(--cs-text-tertiary);
  font-weight: 500;
  letter-spacing: 0.02em;
  text-transform: uppercase;
}
.cs-stat-card__value {
  font-size: var(--cs-font-size-3xl);
  font-weight: 700;
  line-height: var(--cs-line-height-tight);
  color: var(--cs-text-primary);
  letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
}
.cs-stat-card__meta {
  display: flex;
  align-items: center;
  gap: var(--cs-space-2);
  font-size: var(--cs-font-size-sm);
}
.cs-stat-card__trend {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.cs-stat-card__trend.is-up { color: var(--cs-color-accent); }
.cs-stat-card__trend.is-down { color: var(--cs-severity-critical); }
.cs-stat-card__trend.is-neutral { color: var(--cs-text-tertiary); }
.cs-stat-card__hint {
  color: var(--cs-text-tertiary);
}
</style>
