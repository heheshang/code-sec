<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, type Ref } from 'vue'
import * as echarts from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { HeatmapChart, BarChart, LineChart, PieChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  VisualMapComponent,
} from 'echarts/components'

echarts.use([
  CanvasRenderer,
  HeatmapChart,
  BarChart,
  LineChart,
  PieChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  VisualMapComponent,
])

export interface BenchmarkData {
  name: string
  precision: number
  recall: number
  f1: number
  confusionMatrix: number[][]
  sampleCount: number
}

const props = defineProps<{
  benchmarks: BenchmarkData[]
}>()

const chartRef: Ref<HTMLElement | null> = ref(null)
let chart: echarts.ECharts | null = null

function initChart() {
  if (!chartRef.value) return
  chart = echarts.init(chartRef.value)
  renderChart()
}

function renderChart() {
  if (!chart || props.benchmarks.length === 0) return
  const labels = props.benchmarks.map((b) => b.name)
  chart.setOption({
    title: { text: 'Benchmark Results', left: 'center', textStyle: { fontSize: 15 } },
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0, data: ['Precision', 'Recall', 'F1'] },
    grid: { left: 60, right: 30, top: 50, bottom: 50 },
    xAxis: { type: 'category', data: labels, axisLabel: { rotate: 20 } },
    yAxis: { type: 'value', min: 0, max: 1, axisLabel: { formatter: '{value}%' } },
    series: [
      {
        name: 'Precision',
        type: 'bar',
        data: props.benchmarks.map((b) => +(b.precision * 100).toFixed(1)),
        itemStyle: { color: '#5B47E0' },
      },
      {
        name: 'Recall',
        type: 'bar',
        data: props.benchmarks.map((b) => +(b.recall * 100).toFixed(1)),
        itemStyle: { color: '#00B96B' },
      },
      {
        name: 'F1',
        type: 'line',
        data: props.benchmarks.map((b) => +(b.f1 * 100).toFixed(1)),
        itemStyle: { color: '#FA541C' },
        lineStyle: { width: 3 },
        symbol: 'circle',
        symbolSize: 8,
      },
    ],
  })
}

function resize() {
  chart?.resize()
}

onMounted(initChart)
onUnmounted(() => chart?.dispose())
watch(() => props.benchmarks, renderChart, { deep: true })
</script>

<template>
  <div class="cs-benchmark-chart">
    <div v-if="benchmarks.length === 0" class="cs-benchmark-chart-empty">
      <el-empty description="No benchmark data" :image-size="80" />
    </div>
    <div v-else ref="chartRef" class="cs-benchmark-chart-container" />
  </div>
</template>

<style scoped>
.cs-benchmark-chart {
  width: 100%;
}

.cs-benchmark-chart-empty {
  padding: 40px 0;
}

.cs-benchmark-chart-container {
  width: 100%;
  height: 360px;
}
</style>
