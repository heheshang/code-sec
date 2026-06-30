<script setup lang="ts">
import { ref, computed } from 'vue'
import { Radio, Input, Typography, Space, Tag } from 'ant-design-vue'
import { CheckCircleOutlined, CloseCircleOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { AUDIT_ACTION_LABEL, AUDIT_ACTION_DESCRIPTION } from '@/types/audit'
import type { AuditAction } from '@/types/audit'

interface Props {
  modelValue: AuditAction
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:modelValue', value: AuditAction): void
}>()

const actions: { value: AuditAction; icon: typeof CheckCircleOutlined; tone: 'primary' | 'neutral' | 'warning' }[] = [
  { value: 'confirm', icon: CheckCircleOutlined, tone: 'primary' },
  { value: 'false_positive', icon: CloseCircleOutlined, tone: 'neutral' },
  { value: 'need_retest', icon: ReloadOutlined, tone: 'warning' },
]

const selected = computed<AuditAction>(() => props.modelValue)
const reasonDraft = ref<string>('')

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
      <Radio :value="a.value" :checked="selected === a.value" />
      <div class="cs-decision-card__body">
        <div class="cs-decision-card__head">
          <component :is="a.icon" class="cs-decision-card__icon" />
          <span class="cs-decision-card__label">{{ AUDIT_ACTION_LABEL[a.value] }}</span>
        </div>
        <div class="cs-decision-card__desc">
          {{ AUDIT_ACTION_DESCRIPTION[a.value] }}
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
.cs-decision-card.is-selected {
  border-color: var(--cs-color-primary);
  background: var(--cs-color-primary-bg);
  box-shadow: 0 0 0 2px rgba(91, 71, 224, 0.10);
}
.cs-decision-card__body {
  flex: 1;
  min-width: 0;
}
.cs-decision-card__head {
  display: flex;
  align-items: center;
  gap: var(--cs-space-2);
  font-weight: 600;
  color: var(--cs-text-primary);
}
.cs-decision-card__icon {
  font-size: 16px;
  color: var(--cs-color-primary);
}
.cs-decision-card.tone-neutral .cs-decision-card__icon { color: var(--cs-text-tertiary); }
.cs-decision-card.tone-warning .cs-decision-card__icon { color: var(--cs-severity-medium); }
.cs-decision-card__label {
  font-size: var(--cs-font-size-md);
}
.cs-decision-card__desc {
  margin-top: 2px;
  margin-left: 24px;
  font-size: var(--cs-font-size-sm);
  color: var(--cs-text-secondary);
  line-height: var(--cs-line-height-base);
}
</style>
