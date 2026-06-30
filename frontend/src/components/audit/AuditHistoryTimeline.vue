<script setup lang="ts">
import { computed } from 'vue'
import { Timeline, Tag, Typography, Space } from 'ant-design-vue'
import { CheckCircleOutlined, CloseCircleOutlined, ReloadOutlined, UserOutlined } from '@ant-design/icons-vue'
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
  color: string
  dot: typeof CheckCircleOutlined
  action: AuditAction
  record: AuditRecord
  date: string
}

const items = computed<TimelineItem[]>(() =>
  props.records.map((r) => {
    switch (r.action) {
      case 'confirm':
        return {
          color: 'var(--cs-severity-high)',
          dot: CheckCircleOutlined,
          action: r.action,
          record: r,
          date: dayjs(r.auditedAt).format('MMM D, HH:mm'),
        }
      case 'false_positive':
        return {
          color: 'var(--cs-text-tertiary)',
          dot: CloseCircleOutlined,
          action: r.action,
          record: r,
          date: dayjs(r.auditedAt).format('MMM D, HH:mm'),
        }
      case 'need_retest':
        return {
          color: 'var(--cs-severity-medium)',
          dot: ReloadOutlined,
          action: r.action,
          record: r,
          date: dayjs(r.auditedAt).format('MMM D, HH:mm'),
        }
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
      <Typography.Text type="secondary">No audit history yet for this finding.</Typography.Text>
    </div>
    <Timeline v-else mode="left" class="cs-audit-history__timeline">
      <Timeline.Item
        v-for="(item, idx) in items"
        :key="idx"
        :color="item.color"
      >
        <template #dot>
          <component :is="item.dot" :style="{ color: item.color, fontSize: '16px' }" />
        </template>
        <div class="cs-audit-history__card">
          <div class="cs-audit-history__head">
            <Typography.Text class="cs-audit-history__action">{{ actionLabel[item.action] }}</Typography.Text>
            <Space :size="6">
              <Tag bordered><UserOutlined /> {{ item.record.auditorName }}</Tag>
              <Tag bordered color="default">{{ item.date }}</Tag>
            </Space>
          </div>
          <Typography.Paragraph
            v-if="item.record.exploitCondition"
            class="cs-audit-history__body"
          >
            {{ item.record.exploitCondition }}
          </Typography.Paragraph>
          <div v-if="item.record.pocContent" class="cs-audit-history__poc">
            <pre>{{ item.record.pocContent }}</pre>
          </div>
        </div>
      </Timeline.Item>
    </Timeline>
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
  margin: var(--cs-space-1) 0 !important;
  color: var(--cs-text-secondary);
  font-size: var(--cs-font-size-sm);
}
.cs-audit-history__poc {
  margin-top: var(--cs-space-2);
  background: #0F1117;
  border-radius: var(--cs-radius-sm);
  padding: var(--cs-space-2) var(--cs-space-3);
  font-family: var(--cs-font-mono);
  font-size: 12px;
  color: #E5E7EB;
  overflow-x: auto;
  white-space: pre-wrap;
}
</style>
