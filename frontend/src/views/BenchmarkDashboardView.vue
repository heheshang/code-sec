<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import BenchmarkChart from '@/ai-audit/components/BenchmarkChart.vue'
import type { BenchmarkData } from '@/ai-audit/components/BenchmarkChart.vue'
import { http } from '@/api/client'
import { errMsg } from '@/utils/error'

const benchmarks = ref<BenchmarkData[]>([])
const loading = ref(false)
const lastRun = ref<string | null>(null)
const tooltip = ref<string | null>(null)

async function loadBenchmarks() {
  loading.value = true
  try {
    const resp = await http.get<BenchmarkData[]>('/ai/benchmarks')
    benchmarks.value = resp.data
    lastRun.value = new Date().toISOString()
    tooltip.value = null
  } catch (e: unknown) {
    ElMessage.error(errMsg(e))
    tooltip.value = 'Failed to load benchmarks. Ensure the backend is running.'
  } finally {
    loading.value = false
  }
}

async function runBenchmarks() {
  loading.value = true
  try {
    const resp = await http.post<BenchmarkData[]>('/ai/benchmarks/run')
    benchmarks.value = resp.data
    lastRun.value = new Date().toISOString()
    ElMessage.success('Benchmarks completed')
  } catch (e: unknown) {
    ElMessage.error(errMsg(e))
  } finally {
    loading.value = false
  }
}

const avgPrecision = () => benchmarks.value.length > 0
  ? benchmarks.value.reduce((s, b) => s + b.precision, 0) / benchmarks.value.length
  : 0
const avgRecall = () => benchmarks.value.length > 0
  ? benchmarks.value.reduce((s, b) => s + b.recall, 0) / benchmarks.value.length
  : 0
const avgF1 = () => benchmarks.value.length > 0
  ? benchmarks.value.reduce((s, b) => s + b.f1, 0) / benchmarks.value.length
  : 0
const totalSamples = () => benchmarks.value.reduce((s, b) => s + b.sampleCount, 0)

onMounted(loadBenchmarks)
</script>

<template>
  <div class="cs-benchmark">
    <div class="cs-benchmark-header">
      <div>
        <h2 class="cs-benchmark-title">AI Benchmark Dashboard</h2>
        <p v-if="benchmarks.length > 0" class="cs-benchmark-subtitle">
          {{ benchmarks.length }} benchmarks · {{ totalSamples() }} total samples
          <span v-if="lastRun"> · Last run: {{ new Date(lastRun).toLocaleString() }}</span>
        </p>
      </div>
      <el-space :size="8">
        <el-button @click="loadBenchmarks" :loading="loading">
          <el-icon><Refresh /></el-icon>
          Refresh
        </el-button>
        <el-button type="primary" @click="runBenchmarks" :loading="loading">
          Run benchmarks
        </el-button>
      </el-space>
    </div>

    <el-alert
      v-if="tooltip"
      :title="tooltip"
      type="warning"
      show-icon
      :closable="true"
      style="margin-bottom: var(--cs-space-4)"
    />

    <!-- Summary cards -->
    <el-row :gutter="16" class="cs-benchmark-summary">
      <el-col :xs="24" :sm="8">
        <el-card shadow="never" class="cs-benchmark-card">
          <div class="cs-benchmark-card-value" style="color: var(--cs-color-primary)">
            {{ (avgPrecision() * 100).toFixed(1) }}%
          </div>
          <div class="cs-benchmark-card-label">Avg Precision</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card shadow="never" class="cs-benchmark-card">
          <div class="cs-benchmark-card-value" style="color: var(--cs-color-accent)">
            {{ (avgRecall() * 100).toFixed(1) }}%
          </div>
          <div class="cs-benchmark-card-label">Avg Recall</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card shadow="never" class="cs-benchmark-card">
          <div class="cs-benchmark-card-value" style="color: var(--cs-severity-high)">
            {{ (avgF1() * 100).toFixed(1) }}%
          </div>
          <div class="cs-benchmark-card-label">Avg F1 Score</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Chart -->
    <el-card shadow="never" class="cs-benchmark-chart-card">
      <template #header>Benchmark comparison</template>
      <BenchmarkChart :benchmarks="benchmarks" />
    </el-card>

    <!-- Details table -->
    <el-card shadow="never" class="cs-benchmark-table-card">
      <template #header>Detailed results</template>
      <el-table :data="benchmarks" v-if="benchmarks.length > 0" stripe style="width: 100%">
        <el-table-column prop="name" label="Benchmark" min-width="160" />
        <el-table-column label="Precision" width="120">
          <template #default="{ row }: { row: BenchmarkData }">
            <span :style="{ color: row.precision >= 0.8 ? 'var(--cs-color-accent)' : 'var(--cs-severity-high)' }">
              {{ (row.precision * 100).toFixed(1) }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column label="Recall" width="120">
          <template #default="{ row }: { row: BenchmarkData }">
            <span :style="{ color: row.recall >= 0.8 ? 'var(--cs-color-accent)' : 'var(--cs-severity-high)' }">
              {{ (row.recall * 100).toFixed(1) }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column label="F1 Score" width="120">
          <template #default="{ row }: { row: BenchmarkData }">
            <span :style="{ color: row.f1 >= 0.8 ? 'var(--cs-color-accent)' : 'var(--cs-severity-high)' }">
              {{ (row.f1 * 100).toFixed(1) }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="sampleCount" label="Samples" width="100" />
      </el-table>
      <el-empty v-else description="No benchmark data available" :image-size="80" />
    </el-card>
  </div>
</template>

<style scoped>
.cs-benchmark {
  padding: var(--cs-space-6);
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: var(--cs-space-4);
}

.cs-benchmark-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--cs-space-4);
  flex-wrap: wrap;
}

.cs-benchmark-title {
  font-size: var(--cs-font-size-2xl);
  font-weight: 700;
  margin: 0;
}

.cs-benchmark-subtitle {
  margin: var(--cs-space-1) 0 0;
  font-size: var(--cs-font-size-sm);
  color: var(--cs-text-tertiary);
}

.cs-benchmark-summary :deep(.el-card__body) {
  padding: var(--cs-space-4);
  text-align: center;
}

.cs-benchmark-card-value {
  font-size: 32px;
  font-weight: 700;
  font-family: var(--cs-font-mono);
  line-height: 1.2;
}

.cs-benchmark-card-label {
  font-size: var(--cs-font-size-xs);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--cs-text-tertiary);
  margin-top: var(--cs-space-1);
}

.cs-benchmark-chart-card,
.cs-benchmark-table-card {
  background: var(--cs-bg-elevated);
  border: 1px solid var(--cs-border-light);
  border-radius: var(--cs-radius-lg);
}

.cs-benchmark-chart-card :deep(.el-card__body) {
  padding: var(--cs-space-4);
}
</style>
