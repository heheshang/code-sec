<script setup lang="ts">
import { computed } from 'vue'
import { CircleCheck, CircleClose, Refresh } from '@element-plus/icons-vue'
import { AUDIT_ACTION_LABEL, AUDIT_ACTION_DESCRIPTION } from '@/types/audit'
import type { AuditAction } from '@/types/audit'

interface Props {
  modelValue: AuditAction
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:modelValue', value: AuditAction): void
}>()

const actions: { value: AuditAction; icon: object; tone: 'primary' | 'neutral' | 'warning' }[] = [
  { value: 'confirm', icon: CircleCheck, tone: 'primary' },
  { value: 'false_positive', icon: CircleClose, tone: 'neutral' },
  { value: 'need_retest', icon: Refresh, tone: 'warning' },
]

const selected = computed<AuditAction>(() => props.modelValue)

function select(value: AuditAction): void {
  emit('update:modelValue', value)
}
</script>

<template>
  <div class="cs-decision-cards">
    <div
      v-for="a in actions"
      :key="a.value"
      class="cs-decision-card"
      :class="{ 'is-selected': selected === a.value, [`tone-${a.tone}`]: true }"
      @click="select(a.value)"
    >
      <el-radio class="cs-decision-card__radio" :value="a.value" :model-value="selected" />
      <div class="cs-decision-card__body">
        <div class="cs-decision-card__head">
          <el-icon class="cs-decision-card__icon"><component :is="a.icon" /></el-icon>
          <div class="cs-decision-card__text">
            <span class="cs-decision-card__label">{{ AUDIT_ACTION_LABEL[a.value] }}</span>
            <div class="cs-decision-card__desc">{{ AUDIT_ACTION_DESCRIPTION[a.value] }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cs-decision-cards {
  display: grid;
  grid-template-columns: 1fr;
  gap: var(--cs-space-2);
}
.cs-decision-card {
  display: flex;
  align-items: flex-start;
  gap: var(--cs-space-3);
  border: 1px solid var(--cs-border);
  border-radius: var(--cs-radius-md);
  padding: var(--cs-space-3) var(--cs-space-4);
  cursor: pointer;
  background: var(--cs-bg-elevated);
  transition: all var(--cs-duration-fast) var(--cs-ease-out);
}
.cs-decision-card:hover {
  border-color: var(--cs-color-primary);
  background: var(--cs-color-primary-bg-hover);
}
.cs-decision-card:active {
  transform: scale(0.99);
}
.cs-decision-card:focus-visible {
  outline: 2px solid var(--cs-color-primary);
  outline-offset: 2px;
}

/* Base selected — tone overrides below for stronger visual meaning */
.cs-decision-card.is-selected {
  box-shadow: 0 0 0 2px rgba(91, 71, 224, 0.10);
}

/* confirm — accent green (positive: "this is real") */
.cs-decision-card.tone-primary.is-selected {
  border-color: var(--cs-color-accent);
  background: var(--cs-color-accent-bg);
  box-shadow: 0 0 0 2px rgba(0, 185, 107, 0.12);
}
.cs-decision-card.tone-primary.is-selected .cs-decision-card__icon {
  color: var(--cs-color-accent);
}

/* false_positive — neutral gray (dismissal) */
.cs-decision-card.tone-neutral.is-selected {
  border-color: var(--cs-border-strong);
  background: var(--cs-bg-hover);
  box-shadow: 0 0 0 2px rgba(31, 31, 31, 0.06);
}

/* need_retest — amber (attention: needs follow-up) */
.cs-decision-card.tone-warning.is-selected {
  border-color: var(--cs-severity-medium);
  background: var(--cs-severity-medium-bg);
  box-shadow: 0 0 0 2px rgba(250, 173, 20, 0.15);
}
.cs-decision-card__body {
  flex: 1;
  min-width: 0;
}
.cs-decision-card__radio {
  margin-top: 2px;
  pointer-events: none;
}
.cs-decision-card__head {
  display: flex;
  align-items: flex-start;
  gap: var(--cs-space-2);
  color: var(--cs-text-primary);
}
.cs-decision-card__text {
  flex: 1;
  min-width: 0;
}
.cs-decision-card__icon {
  font-size: 16px;
  color: var(--cs-color-primary);
  flex-shrink: 0;
}
.cs-decision-card.tone-neutral .cs-decision-card__icon { color: var(--cs-text-tertiary); }
.cs-decision-card.tone-warning .cs-decision-card__icon { color: var(--cs-severity-medium); }
.cs-decision-card__label {
  font-weight: 600;
  font-size: var(--cs-font-size-md);
}
.cs-decision-card__desc {
  margin-top: 2px;
  font-size: var(--cs-font-size-sm);
  color: var(--cs-text-secondary);
  line-height: var(--cs-line-height-base);
  font-weight: 400;
}
</style>
