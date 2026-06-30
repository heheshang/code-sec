<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Row, Col, Card, Typography, Tag, Space, Empty } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart, LineChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
} from 'echarts/components'
import { http } from '@/api/client'
import StatCard from '@/components/common/StatCard.vue'
import { useVulnStore } from '@/stores/vuln'
import type { RepoListItem } from '@/api/types'

use([
  CanvasRenderer,
  PieChart,
  LineChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
])

interface DashboardStats {
  total: number
  critical: number
  open: number
  fixedThisWeek: number
  fixRate: number
  severityCounts: Record<'critical' | 'high' | 'medium' | 'low' | 'info', number>
  projectCount: number
}

interface TrendPoint {
  date: string
  opened: number
  closed: number
}

interface TrendResponse {
  trend: TrendPoint[]
  tookMs: number
}

const router = useRouter()
const vulnStore = useVulnStore()
const stats = ref<DashboardStats | null>(null)
const trendData = ref<TrendPoint[]>([])
const projects = ref<RepoListItem[]>([])
const loading = ref<boolean>(true)
const error = ref<string | null>(null)
const queueCount = ref<number>(0)

const hasData = computed(() => stats.value !== null)
const hasSeverityData = computed(() => {
  if (stats.value === null) return false
  const c = stats.value.severityCounts
  if (c === undefined) return false
  return c.critical > 0 || c.high > 0 || c.medium > 0 || c.low > 0 || c.info > 0
})
const hasTrend = computed(() => Array.isArray(trendData.value) && trendData.value.length > 0)

const fixRatePct = computed<string>(() => {
  if (stats.value === null) return '0%'
  return `${Math.round(stats.value.fixRate * 100)}%`
})

const pieOption = computed(() => {
  if (stats.value === null) return {}
  const counts = stats.value.severityCounts ?? { critical: 0, high: 0, medium: 0, low: 0, info: 0 }
  return {
    tooltip: {
      trigger: 'item',
      backgroundColor: '#1F1F1F',
      borderColor: '#1F1F1F',
      textStyle: { color: '#fff', fontSize: 12 },
    },
    legend: {
      bottom: 0,
      textStyle: { color: 'var(--cs-text-secondary)', fontSize: 12 },
      icon: 'circle',
      itemHeight: 8,
    },
    series: [
      {
        type: 'pie',
        radius: ['62%', '88%'],
        center: ['50%', '46%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        labelLine: { show: false },
        data: [
          { name: 'Critical', value: counts.critical, itemStyle: { color: '#CF1322' } },
          { name: 'High', value: counts.high, itemStyle: { color: '#FA541C' } },
          { name: 'Medium', value: counts.medium, itemStyle: { color: '#FAAD14' } },
          { name: 'Low', value: counts.low, itemStyle: { color: '#1890FF' } },
          { name: 'Info', value: counts.info, itemStyle: { color: '#8C8C8C' } },
        ],
      },
    ],
  }
})

const lineOption = computed(() => {
  if (trendData.value.length === 0) return {}
  const dates = trendData.value.map((d) => d.date.slice(5))
  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#1F1F1F',
      borderColor: '#1F1F1F',
      textStyle: { color: '#fff', fontSize: 12 },
    },
    legend: {
      data: ['Opened', 'Closed'],
      bottom: 0,
      textStyle: { color: 'var(--cs-text-secondary)', fontSize: 12 },
      icon: 'circle',
      itemHeight: 8,
    },
    grid: { left: 30, right: 16, top: 16, bottom: 36 },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: 'var(--cs-border)' } },
      axisLabel: { color: 'var(--cs-text-tertiary)', fontSize: 11 },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'var(--cs-border-light)' } },
      axisLabel: { color: 'var(--cs-text-tertiary)', fontSize: 11 },
    },
    series: [
      {
        name: 'Opened',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { color: '#FA541C', width: 2 },
        itemStyle: { color: '#FA541C' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(250, 84, 28, 0.18)' },
              { offset: 1, color: 'rgba(250, 84, 28, 0)' },
            ],
          },
        },
        data: trendData.value.map((d) => d.opened),
      },
      {
        name: 'Closed',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { color: '#00B96B', width: 2 },
        itemStyle: { color: '#00B96B' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(0, 185, 107, 0.18)' },
              { offset: 1, color: 'rgba(0, 185, 107, 0)' },
            ],
          },
        },
        data: trendData.value.map((d) => d.closed),
      },
    ],
  }
})

onMounted(async () => {
  loading.value = true
  error.value = null
  try {
    const [statsResp, trendResp, projectsResp, listResp] = await Promise.all([
      http.get<DashboardStats>('/dashboard/stats'),
      http.get<TrendResponse>('/dashboard/trend?days=14'),
      http.get<{ items: RepoListItem[] }>('/repos?page=1&size=100'),
      vulnStore.fetchList(),
    ])
    stats.value = statsResp.data
    trendData.value = trendResp.data.trend
    projects.value = projectsResp.data.items ?? []
    queueCount.value = listResp === undefined ? 0 : vulnStore.queueCount
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to load dashboard data'
  } finally {
    loading.value = false
  }
})

function goToQueue(filter?: 'critical' | 'pending'): void {
  if (filter === 'critical') {
    vulnStore.setFilters({ severity: ['critical'] })
  } else if (filter === 'pending') {
    vulnStore.setFilters({ status: ['pending_audit', 'pending_retest'] })
  }
  router.push('/audit')
}
</script>

<template>
  <div v-if="error && !loading" class="cs-dashboard cs-dashboard__error">
    <Empty description="Failed to load dashboard data">
      <template #extra>
        <a-button type="primary" @click="location.reload()">Retry</a-button>
      </template>
    </Empty>
  </div>
  <div v-else class="cs-dashboard">
    <div class="cs-dashboard-hero">
      <h1>Security overview</h1>
      <p>{{ stats?.projectCount ?? '—' }} projects · {{ stats?.total ?? '—' }} total findings</p>
    </div>

    <Row :gutter="[16, 16]" class="cs-dashboard__stats">
      <Col :xs="24" :sm="12" :md="6">
        <StatCard
          label="Total findings"
          :value="stats?.total ?? '—'"
          :loading="loading"
          hint="across 5 projects"
          accent="primary"
        />
      </Col>
      <Col :xs="24" :sm="12" :md="6">
        <StatCard
          label="Critical"
          :value="stats?.critical ?? '—'"
          :loading="loading"
          hint="requires 24h SLA"
          accent="danger"
        />
      </Col>
      <Col :xs="24" :sm="12" :md="6">
        <StatCard
          label="Fixed this week"
          :value="stats?.fixedThisWeek ?? '—'"
          :loading="loading"
          hint="in last 7 days"
          accent="success"
        />
      </Col>
      <Col :xs="24" :sm="12" :md="6">
        <StatCard
          label="Fix rate"
          :value="fixRatePct"
          :loading="loading"
          hint="rolling 30 days"
          accent="primary"
        />
      </Col>
    </Row>

    <Row :gutter="[16, 16]" class="cs-dashboard__charts">
      <Col :xs="24" :lg="10">
        <Card
          title="Findings by severity"
          :bordered="false"
          class="cs-dashboard__chartCard"
        >
          <div v-if="hasSeverityData" class="cs-dashboard__pie">
            <VChart :option="pieOption" autoresize />
          </div>
          <div v-else-if="!loading" class="cs-dashboard__chartEmpty">
            <Empty image="simple" description="No severity data" />
          </div>
        </Card>
      </Col>
      <Col :xs="24" :lg="14">
        <Card
          title="Opened vs closed · last 14 days"
          :bordered="false"
          class="cs-dashboard__chartCard"
        >
          <div v-if="hasTrend" class="cs-dashboard__line">
            <VChart :option="lineOption" autoresize />
          </div>
          <div v-else-if="!loading" class="cs-dashboard__chartEmpty">
            <Empty image="simple" description="No trend data" />
          </div>
        </Card>
      </Col>
    </Row>

    <Row :gutter="[16, 16]" class="cs-dashboard__projects">
      <Col :span="24">
        <Card title="Projects" :bordered="false" class="cs-dashboard__projectCard">
          <div class="cs-dashboard__projectGrid">
            <div
              v-for="p in projects"
              :key="p.id"
              class="cs-dashboard__projectTile"
              @click="router.push(`/audit?project=${p.id}`)"
            >
              <div class="cs-dashboard__projectHead">
                <Typography.Text class="cs-dashboard__projectName">{{ p.name }}</Typography.Text>
                <Tag :color="p.status === 'active' ? 'green' : 'default'" bordered>{{ p.status }}</Tag>
              </div>
              <div class="cs-dashboard__projectMeta">
                <span>{{ p.platform }}</span>
                <span v-if="p.businessLine">·</span>
                <span>{{ p.businessLine }}</span>
              </div>
            </div>
          </div>
          <div v-if="projects.length === 0 && !loading" class="cs-dashboard__projectEmpty">
            <Empty image="simple" description="No projects found" />
          </div>
        </Card>
      </Col>
    </Row>
  </div>
</template>

<style scoped>
.cs-dashboard__stats {
  margin-bottom: var(--cs-space-4);
}
.cs-dashboard__charts {
  margin-bottom: var(--cs-space-4);
}
.cs-dashboard__chartCard {
  background: var(--cs-bg-elevated);
  border: 1px solid var(--cs-border-light);
  border-radius: var(--cs-radius-lg);
  height: 320px;
}
.cs-dashboard__chartCard :deep(.ant-card-body) {
  height: calc(100% - 56px);
  padding: var(--cs-space-2) var(--cs-space-3);
}
.cs-dashboard__pie {
  height: 100%;
}
.cs-dashboard__line {
  height: 100%;
}
.cs-dashboard__projectCard {
  background: var(--cs-bg-elevated);
  border: 1px solid var(--cs-border-light);
  border-radius: var(--cs-radius-lg);
}
.cs-dashboard__projectGrid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--cs-space-3);
}
.cs-dashboard__projectTile {
  border: 1px solid var(--cs-border-light);
  border-radius: var(--cs-radius-md);
  padding: var(--cs-space-3) var(--cs-space-4);
  cursor: pointer;
  transition: all var(--cs-duration-fast) var(--cs-ease-out);
  background: var(--cs-bg-elevated);
}
.cs-dashboard__projectTile:hover {
  border-color: var(--cs-color-primary);
  background: var(--cs-color-primary-bg);
  transform: translateY(-1px);
  box-shadow: var(--cs-shadow-2);
}
.cs-dashboard__projectHead {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}
.cs-dashboard__projectName {
  font-family: var(--cs-font-mono);
  font-size: var(--cs-font-size-md);
  font-weight: 600;
  color: var(--cs-text-primary);
}
.cs-dashboard__projectMeta {
  font-size: var(--cs-font-size-xs);
  color: var(--cs-text-tertiary);
  display: flex;
  gap: 4px;
  margin-bottom: var(--cs-space-2);
  text-transform: capitalize;
}
.cs-dashboard__projectCounts {
  margin-bottom: var(--cs-space-2);
  display: flex;
  flex-wrap: wrap;
}
.cs-dashboard__projectBar {
  height: 4px;
  background: var(--cs-bg-hover);
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: 4px;
}
.cs-dashboard__projectBarFill {
  height: 100%;
  background: linear-gradient(90deg, var(--cs-color-accent) 0%, var(--cs-color-primary) 100%);
  border-radius: 2px;
  transition: width var(--cs-duration-slow) var(--cs-ease-out);
}
.cs-dashboard__projectBarLabel {
  font-size: var(--cs-font-size-xs);
  color: var(--cs-text-tertiary);
  text-align: right;
}
.cs-dashboard__error {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}
.cs-dashboard__chartEmpty,
.cs-dashboard__projectEmpty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 200px;
}
</style>
