<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  Odometer,
  Tickets,
  FolderOpened,
  Search,
  Warning,
  Document,
  Lock,
  Setting,
} from '@element-plus/icons-vue'
import { useUiStore } from '@/stores/ui'

const ui = useUiStore()
const route = useRoute()

interface MenuItem {
  key: string
  label: string
  icon: string
  to: string
}

const items: MenuItem[] = [
  { key: 'dashboard', label: 'Dashboard', icon: 'Odometer', to: '/dashboard' },
  { key: 'audit', label: 'Audit queue', icon: 'Tickets', to: '/audit' },
  { key: 'repos', label: 'Repositories', icon: 'FolderOpened', to: '/repos' },
  { key: 'scans', label: 'Scans', icon: 'Search', to: '/scans' },
  { key: 'tickets', label: 'Tickets', icon: 'Warning', to: '/tickets' },
  { key: 'reports', label: 'Reports', icon: 'Document', to: '/reports' },
  { key: 'rules', label: 'Rules', icon: 'Lock', to: '/rules' },
  { key: 'search', label: 'Search', icon: 'Search', to: '/search' },
  { key: 'settings', label: 'Settings', icon: 'Setting', to: '/settings' },
]

const activeKey = computed<string>(() => {
  const path = route.path
  const matched = items.find((item) => item.to !== '/' && path.startsWith(item.to))
  return matched?.to ?? ''
})
</script>

<template>
  <div class="cs-sidebar-brand">
    <div class="cs-sidebar-brand__mark">
      <el-icon :size="18"><Lock /></el-icon>
    </div>
    <div v-if="!ui.sidebarCollapsed" class="cs-sidebar-brand__text">
      <div class="cs-sidebar-brand__name">code-sec</div>
      <div class="cs-sidebar-brand__sub">Audit workbench</div>
    </div>
  </div>
  <el-menu
    :default-active="activeKey"
    :collapse="ui.sidebarCollapsed"
    class="cs-sidebar-menu"
    :router="true"
  >
    <el-menu-item v-for="item in items" :key="item.key" :index="item.to">
      <el-icon><component :is="item.icon" /></el-icon>
      <template #title>{{ item.label }}</template>
    </el-menu-item>
  </el-menu>
  <div v-if="!ui.sidebarCollapsed" class="cs-sidebar-footer">
    <div class="cs-sidebar-footer__line">v0.1.0 · M1 prototype</div>
  </div>
</template>

<style scoped>
.cs-sidebar-brand {
  display: flex;
  align-items: center;
  gap: var(--cs-space-3);
  padding: 0 var(--cs-space-4);
  height: var(--cs-header-height);
  border-bottom: 1px solid var(--el-border-color-light);
}
.cs-sidebar-brand__mark {
  width: 32px;
  height: 32px;
  border-radius: var(--cs-radius-md);
  background: linear-gradient(135deg, var(--el-color-primary) 0%, #7D6BE8 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.cs-sidebar-brand__text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
  overflow: hidden;
}
.cs-sidebar-brand__name {
  font-weight: 700;
  font-size: var(--cs-font-size-md);
  letter-spacing: -0.01em;
}
.cs-sidebar-brand__sub {
  font-size: var(--cs-font-size-xs);
  color: var(--cs-text-tertiary);
}

.cs-sidebar-menu {
  padding-top: var(--cs-space-2);
  border-right: none !important;
  background: transparent !important;
}
.cs-sidebar-menu :deep(.el-menu-item) {
  height: 38px;
  line-height: 38px;
  margin: 2px 8px;
  width: calc(100% - 16px);
  border-radius: var(--cs-radius-md);
  font-weight: 500;
}
.cs-sidebar-menu :deep(.el-menu-item.is-active) {
  background: var(--el-color-primary-light-9) !important;
  color: var(--el-color-primary) !important;
}

.cs-sidebar-footer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: var(--cs-space-3) var(--cs-space-4);
  border-top: 1px solid var(--el-border-color-light);
  font-size: var(--cs-font-size-xs);
  color: var(--cs-text-tertiary);
  line-height: 1.6;
}
</style>
