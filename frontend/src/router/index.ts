import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/dashboard',
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
    path: '/rules',
    name: 'rules',
    component: () => import('@/views/RulesView.vue'),
    meta: { title: 'Rule Management' },
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

router.afterEach((to) => {
  const title = to.meta.title
  if (typeof title === 'string') {
    document.title = `code-sec · ${title}`
  }
})
