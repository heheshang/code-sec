<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Row, Col, Card, Typography, Tag, Space } from 'ant-design-vue'
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
import { projects as projectList } from '@/api/mock/data'
import { useVulnStore } from '@/stores/vuln'

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
  trend: { date: string; opened: number; closed: number }[]
  projectCount: number
}

const router = useRouter()
const vulnStore = useVulnStore()
const stats = ref<DashboardStats | null>(null)
const loading = ref<boolean>(true)
const queueCount = ref<number>(0)

const fixRatePct = computed<string>(() => {
  if (stats.value === null) return '0%'
  return `${Math.round(stats.value.fixRate * 100)}%`
})

const pieOption = computed(() => {
  if (stats.value === null) return {}
  const counts = stats.value.severityCounts
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
        ],
      },
    ],
  }
})

const lineOption = computed(() => {
  if (stats.value === null) return {}
  const dates = stats.value.trend.map((d) => d.date.slice(5))
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
        data: stats.value.trend.map((d) => d.opened),
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
        data: stats.value.trend.map((d) => d.closed),
      },
    ],
  }
})

onMounted(async () => {
  loading.value = true
  try {
    const [statsResp, listResp] = await Promise.all([
      http.get<DashboardStats>('/dashboard/stats'),
      vulnStore.fetchList(),
    ])
    stats.value = statsResp.data
    queueCount.value = listResp === undefined ? 0 : vulnStore.queueCount
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
  <div class="cs-dashboard">
    <div class="cs-dashboard-hero">
      <h1>Security overview</h1>
      <p>{{ stats?.projectCount ?? 5 }} projects · {{ stats?.total ?? 0 }} total findings · last scan 28 minutes ago</p>
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
          :trend="12.5"
          :trend-inverse="true"
        />
      </Col>
      <Col :xs="24" :sm="12" :md="6">
        <StatCard
          label="Fixed this week"
          :value="stats?.fixedThisWeek ?? '—'"
          :loading="loading"
          hint="vs 4 last week"
          accent="success"
          :trend="33.3"
        />
      </Col>
      <Col :xs="24" :sm="12" :md="6">
        <StatCard
          label="Fix rate"
          :value="fixRatePct"
          :loading="loading"
          hint="rolling 30 days"
          accent="primary"
          :trend="2.1"
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
          <div class="cs-dashboard__pie">
            <VChart :option="pieOption" autoresize />
          </div>
        </Card>
      </Col>
      <Col :xs="24" :lg="14">
        <Card
          title="Opened vs closed · last 14 days"
          :bordered="false"
          class="cs-dashboard__chartCard"
        >
          <div class="cs-dashboard__line">
            <VChart :option="lineOption" autoresize />
          </div>
        </Card>
      </Col>
    </Row>

    <Row :gutter="[16, 16]" class="cs-dashboard__projects">
      <Col :span="24">
        <Card title="Projects" :bordered="false" class="cs-dashboard__projectCard">
          <div class="cs-dashboard__projectGrid">
            <div
              v-for="p in projectList"
              :key="p.id"
              class="cs-dashboard__projectTile"
              @click="router.push(`/audit?project=${p.id}`)"
            >
              <div class="cs-dashboard__projectHead">
                <Typography.Text class="cs-dashboard__projectName">{{ p.name }}</Typography.Text>
                <Tag :color="p.status === 'active' ? 'green' : 'default'" bordered>{{ p.status }}</Tag>
              </div>
              <div class="cs-dashboard__projectMeta">
                <span>{{ p.language }}</span>
                <span>·</span>
                <span>{{ p.framework }}</span>
                <span>·</span>
                <span>{{ p.businessLine }}</span>
              </div>
              <Space :size="6" class="cs-dashboard__projectCounts">
                <Tag v-if="p.criticalCount > 0" color="red" bordered>{{ p.criticalCount }} critical</Tag>
                <Tag v-if="p.highCount > 0" color="orange" bordered>{{ p.highCount }} high</Tag>
                <Tag v-if="p.mediumCount > 0" color="gold" bordered>{{ p.mediumCount }} medium</Tag>
                <Tag v-if="p.lowCount > 0" color="blue" bordered>{{ p.lowCount }} low</Tag>
              </Space>
              <div class="cs-dashboard__projectBar">
                <div class="cs-dashboard__projectBarFill" :style="{ width: `${Math.round(p.fixRate * 100)}%` }" />
              </div>
              <div class="cs-dashboard__projectBarLabel">
                Fix rate {{ Math.round(p.fixRate * 100) }}%
              </div>
            </div>
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
</style>
