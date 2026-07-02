<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUiStore } from '@/stores/ui'
import SidebarNav from './SidebarNav.vue'
import TopBar from './TopBar.vue'

const route = useRoute()
const ui = useUiStore()

const isLoginPage = computed(() => route.name === 'login')

const siderWidth = computed<string>(() =>
  ui.sidebarCollapsed ? '64px' : '220px',
)
</script>

<template>
  <el-container v-if="isLoginPage" class="cs-app cs-app--blank">
    <el-main>
      <router-view v-slot="{ Component, route }">
        <transition name="cs-page-fade" mode="out-in">
          <component :is="Component" :key="route.fullPath" />
        </transition>
      </router-view>
    </el-main>
  </el-container>
  <el-container v-else class="cs-app">
    <el-aside :width="siderWidth" class="cs-app__sider">
      <div class="cs-app__siderInner">
        <SidebarNav />
      </div>
    </el-aside>
    <el-container direction="vertical">
      <el-header class="cs-app__header">
        <TopBar />
      </el-header>
      <el-main class="cs-app__main">
        <router-view v-slot="{ Component, route }">
          <transition name="cs-page-fade" mode="out-in">
            <component :is="Component" :key="route.fullPath" />
          </transition>
        </router-view>
      </el-main>
      <el-footer class="cs-app__footer">
        code-sec audit workbench · prototype build
      </el-footer>
    </el-container>
  </el-container>
</template>

<style scoped>
.cs-app {
  height: 100vh;
  overflow: hidden;
}
.cs-app__sider {
  transition: width var(--cs-duration-base) var(--cs-ease-out) !important;
}
.cs-app__siderInner {
  position: relative;
  height: 100%;
  display: flex;
  flex-direction: column;
}
.cs-app__header {
  display: flex;
  align-items: center;
  border-bottom: 1px solid var(--el-border-color);
  background: var(--el-bg-color-overlay);
  height: var(--cs-header-height);
  padding: 0 var(--cs-space-6);
}
.cs-app__main {
  background: var(--el-bg-color);
}
</style>
