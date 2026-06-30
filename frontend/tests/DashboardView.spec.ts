import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import Antd from 'ant-design-vue'
import DashboardView from '@/views/DashboardView.vue'
import { useVulnStore } from '@/stores/vuln'

// Stub VChart to a plain div so we don't need the full ECharts runtime.
vi.mock('vue-echarts', () => ({
  default: {
    name: 'VChart',
    props: ['option', 'autoresize'],
    template: '<div class="stub-chart" />',
  },
}))

// Mock the HTTP client to return controlled data.
vi.mock('@/api/client', () => ({
  http: {
    get: vi.fn(),
  },
}))

const mockStats = {
  data: {
    total: 147,
    critical: 12,
    high: 34,
    medium: 56,
    low: 38,
    info: 7,
    open: 43,
    fixedThisWeek: 18,
    fixRate: 0.73,
    severityCounts: { critical: 12, high: 34, medium: 56, low: 38, info: 7 },
    projectCount: 5,
    tookMs: 12,
  },
}

const mockTrend = {
  data: {
    trend: [
      { date: '2026-06-17', opened: 3, closed: 1 },
      { date: '2026-06-18', opened: 5, closed: 2 },
      { date: '2026-06-19', opened: 2, closed: 4 },
      { date: '2026-06-20', opened: 7, closed: 3 },
      { date: '2026-06-21', opened: 4, closed: 5 },
      { date: '2026-06-22', opened: 6, closed: 2 },
      { date: '2026-06-23', opened: 3, closed: 3 },
      { date: '2026-06-24', opened: 8, closed: 6 },
      { date: '2026-06-25', opened: 5, closed: 7 },
      { date: '2026-06-26', opened: 4, closed: 4 },
      { date: '2026-06-27', opened: 9, closed: 5 },
      { date: '2026-06-28', opened: 6, closed: 8 },
      { date: '2026-06-29', opened: 7, closed: 3 },
      { date: '2026-06-30', opened: 2, closed: 6 },
    ],
    tookMs: 8,
  },
}

const mockProjects = {
  data: {
    items: [
      { id: 'proj-a', name: 'user-service', platform: 'github', status: 'active', businessLine: 'core' },
      { id: 'proj-b', name: 'payment-api', platform: 'gitlab', status: 'active', businessLine: 'finance' },
      { id: 'proj-c', name: 'docs-site', platform: 'github', status: 'archived', businessLine: 'docs' },
    ],
  },
}

const { http } = await import('@/api/client')

async function mountDashboard() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const store = useVulnStore()
  store.items = []
  store.total = 0

  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'dashboard', component: DashboardView },
      { path: '/audit', name: 'audit', component: { template: '<div/>' } },
    ],
  })

  const wrapper = mount(DashboardView, {
    global: { plugins: [pinia, router, Antd] },
  })
  return { wrapper, store, router }
}

describe('DashboardView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('shows loading skeletons on mount', async () => {
    vi.mocked(http.get).mockResolvedValue({ data: {} })
    const wrapper = mount(DashboardView, {
      global: { plugins: [createPinia(), createRouter({ history: createMemoryHistory(), routes: [{ path: '/', component: { template: '<div/>' } }] }), Antd] },
    })
    // StatCard renders Skeleton when loading
    const skeleton = wrapper.find('.ant-skeleton')
    expect(skeleton.exists()).toBe(true)
  })

  it('renders stats, trend, and projects when APIs succeed', async () => {
    vi.mocked(http.get)
      .mockResolvedValueOnce(mockStats)
      .mockResolvedValueOnce(mockTrend)
      .mockResolvedValueOnce(mockProjects)

    const { wrapper } = await mountDashboard()
    await flushPromises()

    // Hero section
    expect(wrapper.text()).toContain('5 projects')
    expect(wrapper.text()).toContain('147 total findings')

    // Stat cards
    expect(wrapper.text()).toContain('Total findings')
    expect(wrapper.text()).toContain('147')
    expect(wrapper.text()).toContain('Fixed this week')
    expect(wrapper.text()).toContain('18')
    expect(wrapper.text()).toContain('73%') // fixRate rounded to 73%

    // Chart containers
    const charts = wrapper.findAll('.stub-chart')
    expect(charts.length).toBeGreaterThanOrEqual(1)
  })

  it('renders project tiles from API response', async () => {
    vi.mocked(http.get)
      .mockResolvedValueOnce(mockStats)
      .mockResolvedValueOnce(mockTrend)
      .mockResolvedValueOnce(mockProjects)

    const { wrapper } = await mountDashboard()
    await flushPromises()

    expect(wrapper.text()).toContain('user-service')
    expect(wrapper.text()).toContain('payment-api')
    expect(wrapper.text()).toContain('docs-site')
    expect(wrapper.text()).toContain('active')
    expect(wrapper.text()).toContain('archived')
  })

  it('shows empty chart placeholders when data is missing', async () => {
    // Return stats with zero counts and empty trend
    const emptyStats = {
      data: {
        total: 0, critical: 0, high: 0, medium: 0, low: 0, info: 0,
        open: 0, fixedThisWeek: 0, fixRate: 0,
        severityCounts: { critical: 0, high: 0, medium: 0, low: 0, info: 0 },
        projectCount: 1, tookMs: 1,
      },
    }
    const emptyTrend = { data: { trend: [], tookMs: 0 } }
    const emptyProjects = { data: { items: [] } }

    vi.mocked(http.get)
      .mockResolvedValueOnce(emptyStats)
      .mockResolvedValueOnce(emptyTrend)
      .mockResolvedValueOnce(emptyProjects)

    const { wrapper } = await mountDashboard()
    await flushPromises()

    // Should show empty descriptions
    expect(wrapper.text()).toContain('No severity data')
    expect(wrapper.text()).toContain('No trend data')
  })

  it('shows error state when API call fails', async () => {
    vi.mocked(http.get).mockRejectedValue(new Error('Network error'))

    const { wrapper } = await mountDashboard()
    await flushPromises()

    expect(wrapper.text()).toContain('Failed to load dashboard data')
  })

  it('navigates to audit queue when a project tile is clicked', async () => {
    vi.mocked(http.get)
      .mockResolvedValueOnce(mockStats)
      .mockResolvedValueOnce(mockTrend)
      .mockResolvedValueOnce(mockProjects)

    const { wrapper, router } = await mountDashboard()
    await flushPromises()

    // Click the first project tile
    const tile = wrapper.find('.cs-dashboard__projectTile')
    expect(tile.exists()).toBe(true)
    await tile.trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/audit')
  })
})
