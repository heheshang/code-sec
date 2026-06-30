<script setup lang="ts">
import { Select, Space } from 'ant-design-vue'
import { useSearchStore } from '@/stores/search'

const store = useSearchStore()

const severityOptions = [
  { value: 'critical', label: 'Critical' },
  { value: 'high', label: 'High' },
  { value: 'medium', label: 'Medium' },
  { value: 'low', label: 'Low' },
  { value: 'info', label: 'Info' },
]

const exploitabilityOptions = [
  { value: 'EXPLOITABLE', label: 'Exploitable' },
  { value: 'POTENTIALLY_EXPLOITABLE', label: 'Potentially Exploitable' },
  { value: 'NOT_EXPLOITABLE', label: 'Not Exploitable' },
]

const sortOptions = [
  { value: '_score', label: 'Relevance' },
  { value: 'discovered_at', label: 'Date discovered' },
]
</script>

<template>
  <div class="cs-search-filters">
    <div class="cs-search-filters__group">
      <label class="cs-search-filters__label">Severity</label>
      <Select
        v-model:value="store.severityFilter"
        mode="multiple"
        :options="severityOptions"
        placeholder="All severities"
        allow-clear
        style="width: 100%"
        size="small"
      />
    </div>

    <div class="cs-search-filters__group">
      <label class="cs-search-filters__label">Exploitability</label>
      <Select
        v-model:value="store.exploitabilityFilter"
        mode="multiple"
        :options="exploitabilityOptions"
        placeholder="All"
        allow-clear
        style="width: 100%"
        size="small"
      />
    </div>

    <div class="cs-search-filters__group">
      <label class="cs-search-filters__label">Sort by</label>
      <Select
        v-model:value="store.sortBy"
        :options="sortOptions"
        style="width: 100%"
        size="small"
      />
    </div>

    <div class="cs-search-filters__group">
      <label class="cs-search-filters__label">Order</label>
      <Select
        v-model:value="store.sortOrder"
        :options="[
          { value: 'desc', label: 'Descending' },
          { value: 'asc', label: 'Ascending' },
        ]"
        style="width: 100%"
        size="small"
      />
    </div>

    <a-button
 v-if="store.hasActiveFilters"
 type="link"
 size="small"
 @click="store.clearFilters()"
    >
      Clear all filters
    </a-button>
  </div>
</template>

<style scoped>
.cs-search-filters {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.cs-search-filters__group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.cs-search-filters__label {
  font-size: var(--cs-font-size-xs);
  font-weight: 600;
  color: var(--cs-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
</style>
