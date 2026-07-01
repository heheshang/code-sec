<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Input, Modal } from 'ant-design-vue'
import { SearchOutlined } from '@ant-design/icons-vue'
import { useSearchStore } from '@/stores/search'

const router = useRouter()
const searchStore = useSearchStore()

const visible = ref(false)
const inputRef = ref<InstanceType<typeof Input> | null>(null)

// Recent searches (v1: in-memory only; Saved Searches pushed to Sprint 3 per L3 fix)
const recentSearches = ref<string[]>(['sql injection', 'xss reflected', 'deserialization'])

function open(): void {
  visible.value = true
  // Focus input after modal animation
  setTimeout(() => {
    (inputRef.value as unknown as { focus(): void })?.focus()
  }, 100)
}

function close(): void {
  visible.value = false
}

function doSearch(): void {
  const q = searchStore.query.trim()
  if (!q && !searchStore.hasActiveFilters) return

  // Save to recent searches (simple)
  if (q && !recentSearches.value.includes(q)) {
    recentSearches.value.unshift(q)
    if (recentSearches.value.length > 5) recentSearches.value.pop()
  }

  close()
  router.push({ name: 'search', query: { q: q || undefined } })
}

function selectRecent(q: string): void {
  searchStore.setQuery(q)
  doSearch()
}

function onSaveSearch(): void {
  // UI Hook reserved — Saved Searches pushed to Sprint 3 (per L3 fix)
  // Toast: "保存搜索将在 Sprint 3 提供"
  close()
}

// Expose open/close for parent (TopBar) integration
defineExpose({ open, close })
</script>

<template>
  <Modal
    :open="visible"
    :footer="null"
    :closable="true"
    width="680px"
    class="cs-global-search-modal"
    @cancel="close"
  >
    <div class="cs-global-search">
      <div class="cs-global-search__input-wrapper">
        <Input
          ref="inputRef"
          v-model:value="searchStore.query"
          size="large"
          placeholder="Search vulnerabilities, code snippets, CWE…"
          :prefix="SearchOutlined"
          allow-clear
          class="cs-global-search__input"
          @press-enter="doSearch"
        >
          <template #suffix>
            <kbd class="cs-global-search__kbd">⌘K</kbd>
          </template>
        </Input>
      </div>

      <div v-if="recentSearches.length > 0" class="cs-global-search__recent">
        <div class="cs-global-search__recent-title">Recent searches</div>
        <div
          v-for="item in recentSearches"
          :key="item"
          class="cs-global-search__recent-item"
          @click="selectRecent(item)"
        >
          <SearchOutlined class="cs-global-search__recent-icon" />
          <span>{{ item }}</span>
        </div>
      </div>

      <div class="cs-global-search__actions">
        <a-button type="text" size="small" @click="onSaveSearch">
          Save search (Sprint 3)
        </a-button>
      </div>
    </div>
  </Modal>
</template>

<style scoped>
.cs-global-search {
  padding: 4px 0;
}
.cs-global-search__input-wrapper {
  margin-bottom: 16px;
}
.cs-global-search__input :deep(.ant-input) {
  font-size: 15px;
}
.cs-global-search__kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 22px;
  min-width: 22px;
  padding: 0 6px;
  font-size: 11px;
  font-family: inherit;
  color: var(--cs-text-tertiary);
  background: var(--cs-bg-base);
  border: 1px solid var(--cs-border);
  border-radius: 4px;
  line-height: 1;
}
.cs-global-search__recent {
  margin-bottom: 8px;
}
.cs-global-search__recent-title {
  font-size: 11px;
  font-weight: 600;
  color: var(--cs-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 8px;
}
.cs-global-search__recent-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  color: var(--cs-text-secondary);
  transition: background var(--cs-duration-fast);
}
.cs-global-search__recent-item:hover {
  background: var(--cs-bg-hover);
  color: var(--cs-text-primary);
}
.cs-global-search__recent-icon {
  font-size: 14px;
  color: var(--cs-text-tertiary);
}
.cs-global-search__actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 8px;
  border-top: 1px solid var(--cs-border);
}
</style>
