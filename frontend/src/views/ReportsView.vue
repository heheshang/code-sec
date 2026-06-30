<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Row, Col, Card, Tag, Typography, Space, Button, message } from 'ant-design-vue'
import { FileTextOutlined, ClockCircleOutlined, ThunderboltOutlined, FilePdfOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import PageHeader from '@/components/common/PageHeader.vue'
import { http } from '@/api/client'

interface ReportTemplate {
  id: string
  name: string
  description: string
  frequency: 'monthly' | 'weekly' | 'on-demand'
  lastGeneratedAt: string | null
}

const templates = ref<ReportTemplate[]>([])
const loading = ref<boolean>(true)
const generatingId = ref<string | null>(null)

onMounted(async () => {
  try {
    const resp = await http.get<{ items: ReportTemplate[] }>('/reports')
    templates.value = resp.data.items
  } catch (e) {
    message.error(e instanceof Error ? e.message : 'Failed to load reports')
  } finally {
    loading.value = false
  }
})

function handleGenerate(t: ReportTemplate): void {
  generatingId.value = t.id
  // Simulate async generation
  setTimeout(() => {
    generatingId.value = null
    t.lastGeneratedAt = new Date().toISOString()
    message.success(`${t.name} is being generated. Check your email in ~2 minutes.`)
  }, 900)
}

const frequencyColor: Record<ReportTemplate['frequency'], string> = {
  monthly: 'purple',
  weekly: 'blue',
  'on-demand': 'default',
}
</script>

<template>
  <div class="cs-reports">
    <PageHeader
      title="Reports"
      subtitle="Generate, schedule, and archive security reports for compliance and operations"
    >
      <Tag color="purple" bordered>4 templates</Tag>
    </PageHeader>

    <Row :gutter="[16, 16]">
      <Col v-for="t in templates" :key="t.id" :xs="24" :sm="12" :lg="8">
        <Card :bordered="false" class="cs-reports__card" :loading="loading">
          <template #title>
            <Space :size="8">
              <FileTextOutlined class="cs-reports__icon" />
              <span>{{ t.name }}</span>
            </Space>
          </template>
          <template #extra>
            <Tag :color="frequencyColor[t.frequency]" bordered>{{ t.frequency }}</Tag>
          </template>
          <Typography.Paragraph class="cs-reports__desc">
            {{ t.description }}
          </Typography.Paragraph>
          <div class="cs-reports__meta">
            <ClockCircleOutlined class="cs-reports__metaIcon" />
            <Typography.Text type="secondary" class="cs-reports__metaText">
              <template v-if="t.lastGeneratedAt !== null">
                Last generated {{ dayjs(t.lastGeneratedAt).format('MMM D, HH:mm') }}
              </template>
              <template v-else>
                Not generated yet
              </template>
            </Typography.Text>
          </div>
          <Space :size="6" class="cs-reports__formats">
            <Tag bordered color="default">PDF</Tag>
            <Tag bordered color="default">HTML</Tag>
            <Tag bordered color="default">CSV</Tag>
          </Space>
          <div class="cs-reports__actions">
            <Button
              type="primary"
              :loading="generatingId === t.id"
              @click="handleGenerate(t)"
            >
              <ThunderboltOutlined /> Generate now
            </Button>
            <Button>
              <FilePdfOutlined /> Sample
            </Button>
          </div>
        </Card>
      </Col>
    </Row>
  </div>
</template>

<style scoped>
.cs-reports__card {
  background: var(--cs-bg-elevated);
  border: 1px solid var(--cs-border-light);
  border-radius: var(--cs-radius-lg);
  height: 100%;
  transition: border-color var(--cs-duration-fast);
}
.cs-reports__card:hover {
  border-color: var(--cs-color-primary);
}
.cs-reports__icon {
  color: var(--cs-color-primary);
}
.cs-reports__desc {
  font-size: var(--cs-font-size-sm);
  color: var(--cs-text-secondary);
  line-height: var(--cs-line-height-relaxed);
  min-height: 60px;
}
.cs-reports__meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--cs-font-size-xs);
  color: var(--cs-text-tertiary);
  margin-bottom: var(--cs-space-3);
}
.cs-reports__metaIcon {
  color: var(--cs-text-tertiary);
}
.cs-reports__formats {
  margin-bottom: var(--cs-space-3);
}
.cs-reports__actions {
  display: flex;
  gap: var(--cs-space-2);
}
</style>
