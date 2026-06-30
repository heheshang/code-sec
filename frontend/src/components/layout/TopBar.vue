<script setup lang="ts">
import { computed, h, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Input, Badge, Dropdown, Avatar, Tooltip } from 'ant-design-vue'
import {
  SearchOutlined,
  BellOutlined,
  BulbOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  UserOutlined,
  LogoutOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons-vue'
import { useUiStore } from '@/stores/ui'
import { useGlobalShortcut } from '@/composables/useGlobalShortcut'
import GlobalSearch from '@/components/search/GlobalSearch.vue'

const ui = useUiStore()
const router = useRouter()
const route = useRoute()

const globalSearchRef = ref<InstanceType<typeof GlobalSearch> | null>(null)
const searchShortcutActive = ref(true)

// ⌘K / Ctrl+K → open global search modal
useGlobalShortcut(
  { key: 'k', metaKey: true },
  () => globalSearchRef.value?.open(),
  searchShortcutActive,
)
// Also support Ctrl+K for Windows/Linux
useGlobalShortcut(
  { key: 'k', ctrlKey: true },
  () => globalSearchRef.value?.open(),
  searchShortcutActive,
)

const pageTitle = computed<string>(() => {
  const name = String(route.name ?? 'dashboard')
  switch (name) {
    case 'dashboard': return 'Security overview'
    case 'audit': return 'Audit queue'
    case 'workbench': return 'Audit workbench'
    case 'reports': return 'Reports'
    case 'settings': return 'Settings'
    default: return 'code-sec'
  }
})

interface Notification {
  id: string
  title: string
}

const notifications: Notification[] = [
  { id: 'n1', title: 'New critical finding' },
  { id: 'n2', title: 'Retest ready' },
  { id: 'n3', title: 'Scan complete' },
]

interface UserMenuItem {
  key: string
  label: string
  icon: typeof UserOutlined
}

const userMenuItems: UserMenuItem[] = [
  { key: 'profile', label: 'Profile', icon: UserOutlined },
  { key: 'logout', label: 'Sign out', icon: LogoutOutlined },
]

// AntD's MenuItemType.icon expects a VNode; we wrap each icon component with
// h() so the runtime gets a real VNode.
const userMenuNode = computed(() =>
  userMenuItems.map((m) => ({
    key: m.key,
    label: m.label,
    icon: () => h(m.icon),
  })),
)

const notificationItems = computed(() =>
  notifications.map((n) => ({ key: n.id, label: n.title })),
)

function handleUserClick(e: { key: string | number }): void {
  if (String(e.key) === 'profile') router.push('/settings')
}
</script>

<template>
  <div class="cs-topbar">
    <div class="cs-topbar__left">
      <a-button
        type="text"
        class="cs-topbar__collapse"
        @click="ui.toggleSidebar()"
      >
        <MenuFoldOutlined v-if="!ui.sidebarCollapsed" />
        <MenuUnfoldOutlined v-else />
      </a-button>
      <div class="cs-topbar__title">{{ pageTitle }}</div>
    </div>
    <div class="cs-topbar__center">
      <div class="cs-topbar__search-trigger" @click="globalSearchRef?.open()">
        <SearchOutlined class="cs-topbar__search-icon" />
        <span class="cs-topbar__search-placeholder">Search vulnerabilities…</span>
        <kbd class="cs-topbar__search-kbd">⌘K</kbd>
      </div>
    </div>
    <div class="cs-topbar__right">
      <Tooltip :title="ui.theme === 'dark' ? 'Switch to light' : 'Switch to dark'">
        <a-button type="text" @click="ui.toggleTheme()">
          <BulbOutlined />
        </a-button>
      </Tooltip>
      <Dropdown :menu="{ items: notificationItems }" placement="bottomRight" trigger="click">
        <Badge :count="3" :offset="[-4, 4]" class="cs-topbar__badge">
          <a-button type="text"><BellOutlined /></a-button>
        </Badge>
      </Dropdown>
      <Dropdown
        :menu="{ items: userMenuNode, onClick: handleUserClick }"
        placement="bottomRight"
        trigger="click"
      >
        <div class="cs-topbar__user">
          <Avatar :size="28" class="cs-topbar__avatar">Y</Avatar>
          <div class="cs-topbar__userMeta">
            <div class="cs-topbar__userName">You</div>
            <div class="cs-topbar__userRole"><CheckCircleOutlined /> Security auditor</div>
          </div>
        </div>
      </Dropdown>
    </div>

    <GlobalSearch ref="globalSearchRef" />
  </div>
</template>

<style scoped>
.cs-topbar {
  display: flex;
  align-items: center;
  gap: var(--cs-space-4);
  width: 100%;
  height: 100%;
}
.cs-topbar__left {
  display: flex;
  align-items: center;
  gap: var(--cs-space-2);
  min-width: 0;
  flex: 0 0 auto;
}
.cs-topbar__collapse {
  width: 36px;
  height: 36px;
  color: var(--cs-text-secondary);
}
.cs-topbar__title {
  font-size: var(--cs-font-size-md);
  font-weight: 600;
  color: var(--cs-text-primary);
  white-space: nowrap;
}
.cs-topbar__center {
  flex: 1;
  display: flex;
  justify-content: center;
  max-width: 520px;
  margin: 0 auto;
}
.cs-topbar__search-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  max-width: 420px;
  height: 36px;
  padding: 0 12px;
  background: var(--cs-bg-base);
  border: 1px solid var(--cs-border);
  border-radius: var(--cs-radius-md);
  cursor: pointer;
  transition: border-color var(--cs-duration-fast), box-shadow var(--cs-duration-fast);
}
.cs-topbar__search-trigger:hover {
  border-color: var(--cs-color-primary);
  box-shadow: 0 0 0 2px rgba(91, 71, 224, 0.12);
}
.cs-topbar__search-icon {
  font-size: 14px;
  color: var(--cs-text-tertiary);
  flex-shrink: 0;
}
.cs-topbar__search-placeholder {
  flex: 1;
  font-size: var(--cs-font-size-sm);
  color: var(--cs-text-tertiary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.cs-topbar__search-kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 20px;
  min-width: 20px;
  padding: 0 5px;
  font-size: 10px;
  font-family: inherit;
  color: var(--cs-text-tertiary);
  background: var(--cs-bg-hover);
  border: 1px solid var(--cs-border);
  border-radius: 3px;
  flex-shrink: 0;
}
.cs-topbar__right {
  display: flex;
  align-items: center;
  gap: var(--cs-space-1);
  flex: 0 0 auto;
}
.cs-topbar__badge :deep(.ant-badge-count) {
  box-shadow: 0 0 0 2px var(--cs-bg-elevated);
}
.cs-topbar__user {
  display: flex;
  align-items: center;
  gap: var(--cs-space-2);
  padding: 4px 8px 4px 4px;
  border-radius: var(--cs-radius-md);
  cursor: pointer;
  transition: background var(--cs-duration-fast);
}
.cs-topbar__user:hover {
  background: var(--cs-bg-hover);
}
.cs-topbar__avatar {
  background: linear-gradient(135deg, var(--cs-color-primary) 0%, var(--cs-color-accent) 100%) !important;
  font-weight: 600;
}
.cs-topbar__userMeta {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}
.cs-topbar__userName {
  font-size: var(--cs-font-size-sm);
  font-weight: 600;
}
.cs-topbar__userRole {
  font-size: var(--cs-font-size-xs);
  color: var(--cs-text-tertiary);
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
