<script setup lang="ts">
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
  { value: 'exploitable', label: 'Exploitable' },
  { value: 'potentially_exploitable', label: 'Potentially Exploitable' },
  { value: 'not_exploitable', label: 'Not Exploitable' },
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
      <el-select
        v-model="store.severityFilter"
        multiple
        :options="severityOptions"
        placeholder="All severities"
        clearable
        style="width: 100%"
        size="small"
      />
    </div>

    <div class="cs-search-filters__group">
      <label class="cs-search-filters__label">Exploitability</label>
      <el-select
        v-model="store.exploitabilityFilter"
        multiple
        :options="exploitabilityOptions"
        placeholder="All"
        clearable
        style="width: 100%"
        size="small"
      />
    </div>

    <div class="cs-search-filters__group">
      <label class="cs-search-filters__label">Sort by</label>
      <el-select
        v-model="store.sortBy"
        :options="sortOptions"
        style="width: 100%"
        size="small"
      />
    </div>

    <div class="cs-search-filters__group">
      <label class="cs-search-filters__label">Order</label>
      <el-select
        v-model="store.sortOrder"
        :options="[
          { value: 'desc', label: 'Descending' },
          { value: 'asc', label: 'Ascending' },
        ]"
        style="width: 100%"
        size="small"
      />
    </div>

    <el-button
      v-if="store.hasActiveFilters"
      link
      size="small"
      @click="store.clearFilters()"
    >
      Clear all filters
    </el-button>
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
