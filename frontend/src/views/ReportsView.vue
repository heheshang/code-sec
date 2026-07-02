<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Document, Clock, Promotion } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import PageHeader from '@/components/common/PageHeader.vue'
import { http } from '@/api/client'
import { errMsg } from '@/utils/error'

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
    ElMessage.error(e instanceof Error ? e.message : 'Failed to load reports')
  } finally {
    loading.value = false
  }
})

async function handleGenerate(t: ReportTemplate): Promise<void> {
  generatingId.value = t.id
  try {
    await http.post(`/reports/${t.id}/generate`)
    t.lastGeneratedAt = new Date().toISOString()
    ElMessage.success(`${t.name} is being generated. Check your email in ~2 minutes.`)
  } catch (e: unknown) {
    ElMessage.error(errMsg(e))
  } finally {
    generatingId.value = null
  }
}

type FrequencyType = '' | 'primary' | 'success' | 'danger' | 'warning' | 'info'
const frequencyType: Record<ReportTemplate['frequency'], FrequencyType> = {
  monthly: 'info',
  weekly: 'primary',
  'on-demand': '',
}
</script>

<template>
  <div class="cs-page">
    <PageHeader
      title="Reports"
      subtitle="Generate, schedule, and archive security reports for compliance and operations"
    />

    <div v-loading="loading">
      <template v-if="!loading && templates.length === 0">
        <el-card shadow="never" class="cs-reports__empty">
          <el-empty description="No report templates available. Reports will appear here once configured." />
        </el-card>
      </template>

      <el-row v-else :gutter="16">
        <el-col v-for="t in templates" :key="t.id" :xs="24" :sm="12" :lg="8" style="margin-bottom: 16px">
          <el-card shadow="never" class="cs-reports__card">
            <template #header>
              <div style="display: flex; align-items: center; justify-content: space-between">
                <el-space :size="8">
                  <el-icon class="cs-reports__icon"><Document /></el-icon>
                  <span>{{ t.name }}</span>
                </el-space>
                <el-tag :type="frequencyType[t.frequency]" effect="plain" size="small">{{ t.frequency }}</el-tag>
              </div>
            </template>
            <p class="cs-reports__desc">
              {{ t.description }}
            </p>
            <div class="cs-reports__meta">
              <el-icon :size="14"><Clock /></el-icon>
              <span class="cs-reports__metaText">
                <template v-if="t.lastGeneratedAt !== null">
                  Last generated {{ dayjs(t.lastGeneratedAt).format('MMM D, HH:mm') }}
                </template>
                <template v-else>
                  Not generated yet
                </template>
              </span>
            </div>
            <el-space :size="6" class="cs-reports__formats">
              <el-tag effect="plain" size="small">PDF</el-tag>
              <el-tag effect="plain" size="small">HTML</el-tag>
              <el-tag effect="plain" size="small">CSV</el-tag>
            </el-space>
            <div class="cs-reports__actions">
              <el-button type="primary" :loading="generatingId === t.id" @click="handleGenerate(t)">
                <el-icon><Promotion /></el-icon> Generate now
              </el-button>
              <el-button>
                <el-icon><Document /></el-icon> Sample
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
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
  margin: 0 0 var(--cs-space-3);
}
.cs-reports__meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--cs-font-size-xs);
  color: var(--cs-text-tertiary);
  margin-bottom: var(--cs-space-3);
}
.cs-reports__formats {
  margin-bottom: var(--cs-space-3);
}
.cs-reports__actions {
  display: flex;
  gap: var(--cs-space-2);
}
</style>
