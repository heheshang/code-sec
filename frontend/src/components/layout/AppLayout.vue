<script setup lang="ts">
import { computed } from 'vue'
import { Layout } from 'ant-design-vue'
import { useUiStore } from '@/stores/ui'
import SidebarNav from './SidebarNav.vue'
import TopBar from './TopBar.vue'

const { Sider, Header, Content, Footer } = Layout
const ui = useUiStore()

const siderWidth = computed<number>(() =>
  ui.sidebarCollapsed ? 64 : 220,
)
</script>

<template>
  <Layout class="cs-app">
    <Sider
      :width="siderWidth"
      :collapsed-width="64"
      :collapsed="ui.sidebarCollapsed"
      :trigger="null"
      class="cs-app__sider"
    >
      <div class="cs-app__siderInner">
        <SidebarNav />
      </div>
    </Sider>
    <Layout>
      <Header class="cs-app__header">
        <TopBar />
      </Header>
      <Content class="cs-app__content">
        <router-view v-slot="{ Component, route }">
          <transition name="cs-page-fade" mode="out-in">
            <component :is="Component" :key="route.fullPath" />
          </transition>
        </router-view>
      </Content>
      <Footer class="cs-app__footer">
        code-sec audit workbench · prototype build · data served by MSW
      </Footer>
    </Layout>
  </Layout>
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
  padding: 0;
  display: flex;
  align-items: center;
}
.cs-app__content {
  overflow-y: auto;
  background: var(--cs-bg-base);
}
</style>
