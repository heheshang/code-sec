<script setup lang="ts">
import { computed } from 'vue'
import { CircleCheck, CircleClose, Refresh, User } from '@element-plus/icons-vue'
import type { AuditRecord, AuditAction } from '@/types/audit'
import dayjs from 'dayjs'

interface Props {
  records: AuditRecord[]
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
})

interface TimelineItem {
  type: 'primary' | 'success' | 'warning' | 'danger' | 'info'
  dot: object
  action: AuditAction
  record: AuditRecord
  date: string
}

const items = computed<TimelineItem[]>(() =>
  (props.records ?? []).map((r) => {
    switch (r.action) {
      case 'confirm':
        return {
          type: 'danger',
          dot: CircleCheck,
          action: r.action,
          record: r,
          date: dayjs(r.auditedAt).format('MMM D, HH:mm'),
        } as TimelineItem
      case 'false_positive':
        return {
          type: 'info',
          dot: CircleClose,
          action: r.action,
          record: r,
          date: dayjs(r.auditedAt).format('MMM D, HH:mm'),
        } as TimelineItem
      case 'need_retest':
        return {
          type: 'warning',
          dot: Refresh,
          action: r.action,
          record: r,
          date: dayjs(r.auditedAt).format('MMM D, HH:mm'),
        } as TimelineItem
    }
  }),
)

const actionLabel: Record<AuditAction, string> = {
  confirm: 'Confirmed as vulnerability',
  false_positive: 'Marked as false positive',
  need_retest: 'Requested retest',
}
</script>

<template>
  <div class="cs-audit-history">
    <div v-if="loading" class="cs-audit-history__loading">Loading history…</div>
    <div v-else-if="records.length === 0" class="cs-audit-history__empty">
      <span class="cs-audit-history__empty-text">No audit history yet for this finding.</span>
    </div>
    <el-timeline v-else class="cs-audit-history__timeline">
      <el-timeline-item
        v-for="(item, idx) in items"
        :key="idx"
        :type="item.type"
        :timestamp="item.date"
        placement="top"
      >
        <template #dot>
          <el-icon :size="16"><component :is="item.dot" /></el-icon>
        </template>
        <div class="cs-audit-history__card">
          <div class="cs-audit-history__head">
            <span class="cs-audit-history__action">{{ actionLabel[item.action] }}</span>
            <el-space :size="6">
              <el-tag effect="plain"><el-icon :size="12"><User /></el-icon> {{ item.record.auditorName }}</el-tag>
              <el-tag effect="plain">{{ item.date }}</el-tag>
            </el-space>
          </div>
          <p v-if="item.record.exploitCondition" class="cs-audit-history__body">
            {{ item.record.exploitCondition }}
          </p>
          <div v-if="item.record.pocContent" class="cs-audit-history__poc">
            <pre>{{ item.record.pocContent }}</pre>
          </div>
        </div>
      </el-timeline-item>
    </el-timeline>
  </div>
</template>

<style scoped>
.cs-audit-history {
  padding: var(--cs-space-2) var(--cs-space-3);
}
.cs-audit-history__loading,
.cs-audit-history__empty {
  text-align: center;
  padding: var(--cs-space-6) 0;
}
.cs-audit-history__empty-text {
  color: var(--cs-text-tertiary);
}
.cs-audit-history__timeline {
  margin-top: var(--cs-space-2);
}
.cs-audit-history__card {
  background: var(--cs-bg-elevated);
  border: 1px solid var(--cs-border-light);
  border-radius: var(--cs-radius-md);
  padding: var(--cs-space-3) var(--cs-space-4);
}
.cs-audit-history__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: var(--cs-space-2);
  margin-bottom: var(--cs-space-1);
}
.cs-audit-history__action {
  font-weight: 600;
  font-size: var(--cs-font-size-md);
}
.cs-audit-history__body {
  margin: var(--cs-space-1) 0;
  color: var(--cs-text-secondary);
  font-size: var(--cs-font-size-sm);
}
.cs-audit-history__poc {
  margin-top: var(--cs-space-2);
  background: var(--cs-bg-code, #0F1117);
  border-radius: var(--cs-radius-sm);
  padding: var(--cs-space-2) var(--cs-space-3);
  font-family: var(--cs-font-mono);
  font-size: 12px;
  color: var(--cs-text-code, #E5E7EB);
  overflow-x: auto;
  white-space: pre-wrap;
}
</style>
