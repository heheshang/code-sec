<script setup lang="ts">
import { computed, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Menu } from 'ant-design-vue'
import type { MenuItemType } from 'ant-design-vue/es/menu/src/hooks/useItems'
import {
  DashboardOutlined,
  AuditOutlined,
  FileTextOutlined,
  SettingOutlined,
  SafetyCertificateOutlined,
  CodeOutlined,
  ScanOutlined,
  BarsOutlined,
  SearchOutlined,
} from '@ant-design/icons-vue'
import { useUiStore } from '@/stores/ui'

const ui = useUiStore()
const route = useRoute()
const router = useRouter()

interface MenuItem {
  key: string
  label: string
  icon: typeof DashboardOutlined
  to: string
}

const items = computed<MenuItem[]>(() => [
  { key: 'dashboard', label: 'Dashboard', icon: DashboardOutlined, to: '/dashboard' },
  { key: 'audit', label: 'Audit queue', icon: AuditOutlined, to: '/audit' },
  { key: 'repos', label: 'Repositories', icon: CodeOutlined, to: '/repos' },
  { key: 'scans', label: 'Scans', icon: ScanOutlined, to: '/scans' },
  { key: 'tickets', label: 'Tickets', icon: BarsOutlined, to: '/tickets' },
  { key: 'reports', label: 'Reports', icon: FileTextOutlined, to: '/reports' },
  { key: 'rules', label: 'Rules', icon: SafetyCertificateOutlined, to: '/rules' },
  { key: 'search', label: 'Search', icon: SearchOutlined, to: '/search' },
  { key: 'settings', label: 'Settings', icon: SettingOutlined, to: '/settings' },
])

const menuItems = computed(() =>
  items.value.map((i) => ({
    key: i.key,
    label: i.label,
    icon: () => h(i.icon),
  })),
)

void (null as unknown as MenuItemType)

const selectedKey = computed<string[]>(() => {
  const name = String(route.name ?? 'dashboard')
  return [name]
})

function handleClick(e: { key: string | number }): void {
  const found = items.value.find((i) => i.key === String(e.key))
  if (found !== undefined) router.push(found.to)
}
</script>

<template>
  <div class="cs-sidebar-brand">
    <div class="cs-sidebar-brand__mark">
      <SafetyCertificateOutlined />
    </div>
    <div v-if="!ui.sidebarCollapsed" class="cs-sidebar-brand__text">
      <div class="cs-sidebar-brand__name">code-sec</div>
      <div class="cs-sidebar-brand__sub">Audit workbench</div>
    </div>
  </div>
  <Menu
    mode="inline"
    :selected-keys="selectedKey"
    :inline-collapsed="ui.sidebarCollapsed"
    :items="menuItems"
    class="cs-sidebar-menu"
    @click="handleClick"
  />
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
  border-bottom: 1px solid var(--cs-border-light);
}
.cs-sidebar-brand__mark {
  width: 32px;
  height: 32px;
  border-radius: var(--cs-radius-md);
  background: linear-gradient(135deg, var(--cs-color-primary) 0%, var(--cs-color-primary-hover) 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
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
  border-inline-end: none !important;
  background: transparent !important;
}
.cs-sidebar-menu :deep(.ant-menu-item) {
  height: 38px;
  line-height: 38px;
  margin: 2px 8px;
  width: calc(100% - 16px);
  border-radius: var(--cs-radius-md);
  font-weight: 500;
}
.cs-sidebar-menu :deep(.ant-menu-item-selected) {
  background: var(--cs-color-primary-bg) !important;
  color: var(--cs-color-primary) !important;
}
.cs-sidebar-menu :deep(.ant-menu-item-selected::after) {
  display: none;
}
.cs-sidebar-menu :deep(.ant-menu-item-icon) {
  font-size: 16px;
}

.cs-sidebar-footer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: var(--cs-space-3) var(--cs-space-4);
  border-top: 1px solid var(--cs-border-light);
  font-size: var(--cs-font-size-xs);
  color: var(--cs-text-tertiary);
  line-height: 1.6;
}
</style>
