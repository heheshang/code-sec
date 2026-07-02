<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Search,
  Bell,
  Moon,
  Sunny,
  Fold,
  Expand,
  User,
  SwitchButton,
  CircleCheck,
} from '@element-plus/icons-vue'
import { useUiStore } from '@/stores/ui'
import { useGlobalShortcut } from '@/composables/useGlobalShortcut'
import GlobalSearch from '@/components/search/GlobalSearch.vue'

const ui = useUiStore()
const router = useRouter()
const route = useRoute()

const globalSearchRef = ref<InstanceType<typeof GlobalSearch> | null>(null)
const searchShortcutActive = ref(true)

useGlobalShortcut(
  { key: 'k', metaKey: true },
  () => globalSearchRef.value?.open(),
  searchShortcutActive,
)
useGlobalShortcut(
  { key: 'k', ctrlKey: true },
  () => globalSearchRef.value?.open(),
  searchShortcutActive,
)

function handleUserCommand(command: string): void {
  if (command === 'profile') router.push('/settings')
}
</script>

<template>
  <div class="cs-topbar">
    <div class="cs-topbar__left">
      <el-button text class="cs-topbar__collapse" @click="ui.toggleSidebar()">
        <el-icon><Fold v-if="!ui.sidebarCollapsed" /><Expand v-else /></el-icon>
      </el-button>
      <div class="cs-topbar__title">{{ route.meta.title ?? 'code-sec' }}</div>
    </div>
    <div class="cs-topbar__center">
      <div class="cs-topbar__search-trigger" @click="globalSearchRef?.open()">
        <el-icon class="cs-topbar__search-icon"><Search /></el-icon>
        <span class="cs-topbar__search-placeholder">Search vulnerabilities…</span>
        <kbd class="cs-topbar__search-kbd">⌘K</kbd>
      </div>
    </div>
    <div class="cs-topbar__right">
      <el-tooltip :content="ui.theme === 'dark' ? 'Switch to light' : 'Switch to dark'">
        <el-button text @click="ui.toggleTheme()">
          <el-icon><Moon v-if="ui.theme === 'dark'" /><Sunny v-else /></el-icon>
        </el-button>
      </el-tooltip>

      <el-dropdown trigger="click" placement="bottom-end">
        <el-badge :value="3" class="cs-topbar__badge">
          <el-button text><el-icon><Bell /></el-icon></el-button>
        </el-badge>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item>New critical finding</el-dropdown-item>
            <el-dropdown-item>Retest ready</el-dropdown-item>
            <el-dropdown-item>Scan complete</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>

      <el-dropdown trigger="click" placement="bottom-end" @command="handleUserCommand">
        <div class="cs-topbar__user">
          <el-avatar :size="28" class="cs-topbar__avatar">Y</el-avatar>
          <div class="cs-topbar__userMeta">
            <div class="cs-topbar__userName">You</div>
            <div class="cs-topbar__userRole"><el-icon :size="12"><CircleCheck /></el-icon> Security auditor</div>
          </div>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile"><el-icon><User /></el-icon> Profile</el-dropdown-item>
            <el-dropdown-item command="logout"><el-icon><SwitchButton /></el-icon> Sign out</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
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
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 2px rgba(91, 71, 224, 0.12);
}
.cs-topbar__search-icon {
  font-size: 14px;
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
.cs-topbar__badge :deep(.el-badge__content) {
  box-shadow: 0 0 0 2px var(--el-bg-color-overlay);
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
.cs-topbar__avatar :deep(.el-avatar) {
  background: linear-gradient(135deg, var(--el-color-primary) 0%, #7D6BE8 100%) !important;
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
