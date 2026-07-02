<script setup lang="ts">
import type { VNode } from 'vue'

interface Crumb {
  label: string
  to?: string
}

interface Props {
  title: string
  subtitle?: string
  crumbs?: Crumb[]
}

withDefaults(defineProps<Props>(), {
  subtitle: '',
  crumbs: () => [],
})

defineSlots<{
  default?: () => VNode[]
  extra?: () => VNode[]
}>()
</script>

<template>
  <div class="cs-page-header">
    <div class="cs-page-header__main">
      <el-breadcrumb v-if="crumbs.length > 0" class="cs-page-header__crumbs">
        <el-breadcrumb-item v-for="(c, i) in crumbs" :key="i">
          <router-link v-if="c.to" :to="c.to">{{ c.label }}</router-link>
          <span v-else>{{ c.label }}</span>
        </el-breadcrumb-item>
      </el-breadcrumb>
      <el-space :size="12" alignment="center">
        <h3 class="cs-page-header__title">{{ title }}</h3>
        <slot />
      </el-space>
      <span v-if="subtitle" class="cs-page-header__subtitle">
        {{ subtitle }}
      </span>
    </div>
    <div v-if="$slots.extra" class="cs-page-header__extra">
      <slot name="extra" />
    </div>
  </div>
</template>

<style scoped>
.cs-page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--cs-space-4);
  padding-bottom: var(--cs-space-4);
  margin-bottom: var(--cs-space-4);
  border-bottom: 1px solid var(--cs-border-light);
}
.cs-page-header__main {
  flex: 1;
  min-width: 0;
}
.cs-page-header__crumbs {
  margin-bottom: var(--cs-space-2);
  font-size: var(--cs-font-size-sm);
}
.cs-page-header__title {
  margin: 0 !important;
  font-size: var(--cs-font-size-2xl) !important;
  font-weight: 600;
  letter-spacing: -0.01em;
  line-height: var(--cs-line-height-tight);
}
.cs-page-header__subtitle {
  display: block;
  margin-top: var(--cs-space-1);
  color: var(--cs-text-tertiary);
  font-size: var(--cs-font-size-sm);
}
.cs-page-header__extra {
  display: flex;
  gap: var(--cs-space-2);
  align-items: center;
  flex-shrink: 0;
}
</style>
