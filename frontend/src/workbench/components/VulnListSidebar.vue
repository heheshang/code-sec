<script setup lang="ts">
defineProps<{
  items: Array<{
    id: string
    title: string
    severity: string
    filePath: string
    status?: string
  }>
  selectedId?: string
}>()

defineEmits<{
  select: [id: string]
}>()
</script>

<template>
  <div class="cs-vuln-sidebar">
    <div class="cs-vuln-sidebar-header">
      <span class="cs-vuln-sidebar-title">Vulnerabilities</span>
      <el-tag size="small" type="info">{{ items.length }}</el-tag>
    </div>

    <div class="cs-vuln-sidebar-search">
      <el-input
        size="small"
        placeholder="Filter..."
        :prefix-icon="'Search'"
        clearable
      />
    </div>

    <div class="cs-vuln-sidebar-list">
      <div
        v-for="item in items"
        :key="item.id"
        class="cs-vuln-sidebar-item"
        :class="{ active: item.id === selectedId }"
        @click="$emit('select', item.id)"
      >
        <el-tag
          :type="item.severity === 'critical' ? 'danger' : item.severity === 'high' ? 'warning' : 'info'"
          size="small"
          effect="plain"
        >
          {{ item.severity }}
        </el-tag>
        <div class="cs-vuln-sidebar-item-body">
          <div class="cs-vuln-sidebar-item-title">{{ item.title }}</div>
          <div class="cs-vuln-sidebar-item-path">{{ item.filePath }}</div>
        </div>
        <el-tag v-if="item.status" size="small" type="info" effect="plain">
          {{ item.status }}
        </el-tag>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cs-vuln-sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.cs-vuln-sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  border-bottom: 1px solid var(--cs-border);
}

.cs-vuln-sidebar-title {
  font-size: 14px;
  font-weight: 600;
}

.cs-vuln-sidebar-search {
  padding: 8px 12px;
}

.cs-vuln-sidebar-list {
  flex: 1;
  overflow-y: auto;
}

.cs-vuln-sidebar-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 8px 12px;
  cursor: pointer;
  border-bottom: 1px solid var(--cs-border-light);
  transition: background 0.15s;
}

.cs-vuln-sidebar-item:hover {
  background: var(--cs-bg-hover);
}

.cs-vuln-sidebar-item.active {
  background: var(--cs-color-primary-bg);
}

.cs-vuln-sidebar-item-body {
  flex: 1;
  min-width: 0;
}

.cs-vuln-sidebar-item-title {
  font-size: 13px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cs-vuln-sidebar-item-path {
  font-size: 11px;
  color: var(--cs-text-tertiary);
  font-family: var(--cs-font-mono);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
