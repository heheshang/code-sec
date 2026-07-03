import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/dashboard',
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: 'Login' },
  },
  {
    path: '/dashboard',
    name: 'dashboard',
    component: () => import('@/views/DashboardView.vue'),
    meta: { title: 'Security overview' },
  },
  {
    path: '/audit',
    name: 'audit',
    component: () => import('@/views/AuditQueueView.vue'),
    meta: { title: 'Audit queue' },
  },
  {
    path: '/audit/:vulnId',
    name: 'workbench',
    component: () => import('@/views/WorkbenchView.vue'),
    props: true,
    meta: { title: 'Audit workbench' },
  },
  {
    path: '/workbench/:vulnId',
    name: 'audit-workbench',
    component: () => import('@/workbench/components/AuditWorkbench.vue'),
    props: true,
    meta: { title: 'Advanced workbench' },
  },
  {
    path: '/reports',
    name: 'reports',
    component: () => import('@/views/ReportsView.vue'),
    meta: { title: 'Reports' },
  },
  {
    path: '/search',
    name: 'search',
    component: () => import('@/views/SearchResultsView.vue'),
    meta: { title: 'Search' },
  },
  {
    path: '/repos',
    name: 'repos',
    component: () => import('@/views/ReposView.vue'),
    meta: { title: 'Repositories' },
  },
  {
    path: '/scans',
    name: 'scans',
    component: () => import('@/views/ScansView.vue'),
    meta: { title: 'Scans' },
  },
  {
    path: '/tickets',
    name: 'tickets',
    component: () => import('@/views/TicketsView.vue'),
    meta: { title: 'Tickets' },
  },
  {
    path: '/rules',
    name: 'rules',
    component: () => import('@/views/RulesView.vue'),
    meta: { title: 'Rule Management' },
  },
  {
    path: '/ai/benchmark',
    name: 'ai-benchmark',
    component: () => import('@/views/BenchmarkDashboardView.vue'),
    meta: { title: 'AI Benchmark' },
  },
  {
    path: '/settings',
    name: 'settings',
    component: () => import('@/views/SettingsView.vue'),
    meta: { title: 'Settings' },
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard',
  },
]

export const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(_to, _from, saved) {
    return saved ?? { top: 0 }
  },
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  if (to.name === 'login') {
    next()
    return
  }
  if (!token) {
    next({ name: 'login', query: { redirect: to.fullPath } })
    return
  }
  next()
})

router.afterEach((to) => {
  const title = to.meta.title
  if (typeof title === 'string') {
    document.title = `code-sec · ${title}`
  }
})
