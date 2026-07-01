<script setup lang="ts">
import { onMounted, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import { Tabs, Pagination, Spin, Alert, Empty } from 'ant-design-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import VulnSearchResultItem from '@/components/search/VulnSearchResultItem.vue'
import SnippetSearchResultItem from '@/components/search/SnippetSearchResultItem.vue'
import SearchFilters from '@/components/search/SearchFilters.vue'
import { useSearchStore, type SearchTab } from '@/stores/search'

const route = useRoute()
const store = useSearchStore()

const tabs = [
  { key: 'vulns' as SearchTab, label: 'Vulnerabilities' },
  { key: 'snippets' as SearchTab, label: 'Code Snippets' },
]

const totalResults = computed(() => {
  return store.activeTab === 'vulns' ? store.vulnTotal : store.snippetTotal
})

const resultCountLabel = computed(() => {
  if (store.loading) return 'Searching…'
  const n = totalResults.value
  if (n === 0) return 'No results'
  return `${n.toLocaleString()} result${n !== 1 ? 's' : ''} (${store.vulnTookMs}ms)`
})

onMounted(() => {
  // Restore query from URL params
  const q = route.query.q as string | undefined
  if (q) {
    store.setQuery(q)
  }
  const tab = route.query.tab as SearchTab | undefined
  if (tab === 'snippets') {
    store.setTab('snippets')
  }
  store.search()
})

// Re-search when tab or page changes
watch(() => store.activeTab, () => store.search())
watch(() => store.page, () => store.search())
watch(() => store.pageSize, () => store.search())
watch([() => store.severityFilter, () => store.exploitabilityFilter, () => store.sortBy, () => store.sortOrder],
  () => store.search(), { deep: true }
)

function onTabChange(key: string | number): void {
  store.setTab(key as SearchTab)
}

function onPageChange(p: number, ps: number): void {
  store.setPageSize(ps)
  store.setPage(p)
  store.search()
}
</script>

<template>
  <div class="cs-search-page">
    <PageHeader
      :title="`Search: ${store.query || 'All vulnerabilities'}`"
      :subtitle="resultCountLabel"
    />

    <div class="cs-search-page__layout">
      <aside class="cs-search-page__sidebar">
        <SearchFilters />
      </aside>

      <main class="cs-search-page__main">
        <Tabs
          :active-key="store.activeTab"
          :items="tabs.map(t => ({ key: t.key, label: `${t.label} (${t.key === 'vulns' ? store.vulnTotal : store.snippetTotal})` }))"
          @change="onTabChange"
        />

        <Spin :spinning="store.loading" tip="Searching…">
          <Alert
            v-if="store.error"
            type="error"
            :message="store.error"
            show-icon
            class="cs-search-page__error"
          />

          <!-- Vuln results -->
          <div v-if="store.activeTab === 'vulns' && !store.loading" class="cs-search-page__results">
            <Empty
              v-if="store.vulnResults.length === 0"
              description="No vulnerabilities found. Try different keywords or clear filters."
            />
            <VulnSearchResultItem
              v-for="item in store.vulnResults"
              :key="item.id"
              :item="item"
              :highlight-fields="store.vulnHighlights"
            />
          </div>

          <!-- Snippet results -->
          <div v-if="store.activeTab === 'snippets' && !store.loading" class="cs-search-page__results">
            <Empty
              v-if="store.snippetResults.length === 0"
              description="No snippets found. v1 supports file_path prefix search only."
            />
            <SnippetSearchResultItem
              v-for="item in store.snippetResults"
              :key="item.filePath"
              :item="item"
            />
          </div>

          <!-- Pagination -->
          <div v-if="totalResults > 0" class="cs-search-page__pagination">
            <Pagination
              :current="store.page"
              :page-size="store.pageSize"
              :total="totalResults"
              :page-size-options="['20', '50', '100']"
              show-size-changer
              show-quick-jumper
              @change="onPageChange"
            />
          </div>
        </Spin>
      </main>
    </div>
  </div>
</template>

<style scoped>
.cs-search-page {
  height: 100%;
  overflow-y: auto;
}
.cs-search-page__layout {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 24px;
  padding: 0 24px 24px;
}
.cs-search-page__sidebar {
  min-width: 0;
}
.cs-search-page__main {
  min-width: 0;
}
.cs-search-page__results {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 16px;
}
.cs-search-page__error {
  margin-bottom: 16px;
}
.cs-search-page__pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding: 16px 0;
}

@media (max-width: 768px) {
  .cs-search-page__layout {
    grid-template-columns: 1fr;
  }
  .cs-search-page__sidebar {
    display: none;
  }
}
</style>
