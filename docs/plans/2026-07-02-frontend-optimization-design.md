# Frontend Comprehensive Optimization Design

**Date:** 2026-07-02
**Project:** code-sec
**Approach:** Bottom-up serial (Phase 1→2→3→4)
**Status:** Design approved

---

## Phase 1 — Type Safety & Dead Code Elimination

### 1.1 Fix `as any` type casts
- **Problem:** All views use `statusType[row.status] as any` to bypass type checking
- **Fix:** Strictly type `statusType` maps as `Record<string, 'primary' | 'success' | 'danger' | 'warning' | 'info' | ''>`
- **Affected files:**
  - `ScansView.vue:119` — `(statusType[row.status] as any)`
  - `VulnTable.vue:92` — `(statusColorMap[row.status] as any)`
  - `ReposView.vue:146` — `(statusType[row.status] as any)`
  - `TicketsView.vue:92,153` — `(statusType[row.status] as any)`
  - `RulesView.vue:172,224` — `(severityType[row.severity] as any)`
  - `ReportsView.vue:71` — `(frequencyType[t.frequency] as any)`

### 1.2 Fix `catch (e: any)` patterns
- **Problem:** Inconsistent error handling, some use `catch (e: any)` others `catch (e)` with `instanceof`
- **Fix:** Extract `errMsg(e: unknown): string` utility, use `catch (e: unknown) { ElMessage.error(errMsg(e)) }`
- **Affected files:** ScansView, ReposView, RulesView, SettingsView (note the `e: any` pattern)

### 1.3 Dead code removal

| File | Dead code | Action |
|------|-----------|--------|
| `ReposView.vue:11-19` | `columns` array | Remove |
| `ReposView.vue:123-127` | `:pagination` prop on `<el-table>` | Remove (invalid prop) |
| `AuditQueueView.vue:15-18` | `showBulkDrawer` broken getter/setter | Replace with `ref(false)` |
| `VulnTable.vue:14-16` | `projectNameMap` returning `{}` | Remove |
| `vuln.ts:166-167` | `void SEVERITY_ORDER` / `void vulnFromApi` | Remove |

### 1.4 Theme unification
- **Problem:** Two parallel dark mode mechanisms — `data-cs-theme` attribute (`ui.ts`) and `.dark` class (Element Plus)
- **Fix:** Remove `data-cs-theme` from `ui.ts` and `global.css`, use only Element Plus `.dark` class

### 1.5 Miscellaneous
- `LoginView.vue:76` — Remove hardcoded demo credentials from template, move to comment
- `ReportsView.vue:32-38` — Replace 900ms mock `setTimeout` with proper API call
- `SettingsView.vue:10-15` — Extract hardcoded profile mock data from `<script>` to comment

---

## Phase 2 — Store Layer Refactoring

### 2.1 Create `composables/useCrudStore.ts`
- Generic composable for standard CRUD patterns
- Standardized pagination, loading, error states
- Shared `fetchList`, `getById`, `create`, `update`, `remove`, `setPage`, `setPageSize`

```typescript
// Target API
interface CrudStoreOptions<T> {
  baseUrl: string
  pageSize?: number
  transform?: (raw: unknown) => T
}
// Returns standardized state + CRUD methods + pagination
```

### 2.2 Refactor stores
- `scan.ts` — Inherit CRUD from composable, keep `cancel()` specific logic
- `ticket.ts` — Inherit CRUD, keep `transition()` and `assign()` specifics
- `repo.ts` — Inherit CRUD, keep `testConnection()` specifics
- `vuln.ts` — Skip refactor (too many custom filters and methods)
- `search.ts` — Skip refactor (entirely custom search API)

---

## Phase 3 — UI/UX Optimization

### 3.1 Reusable EmptyState component
- Extract from current inline `<el-empty>` usages
- Support configurable: imageSize, description, action button slot
- Standard margin/padding tokens

### 3.2 Skeleton components
- `SkeletonTable.vue` — Table placeholder with configurable rows/columns
- `SkeletonCard.vue` — Card skeleton with header and body
- Replace `v-loading` on `<el-card>` with `<SkeletonTable>` for better perceived performance

### 3.3 Global loading progress bar
- Integrate NProgress-style progress bar in `router/index.ts`
- `router.beforeEach` → start, `router.afterEach` → done
- Use Element Plus's built-in loading or minimal CSS implementation

### 3.4 Dark mode unification
- Remove `data-cs-theme` references in `ui.ts` and `global.css`
- Toggle `.dark` class on `<html>` via `document.documentElement.classList.toggle('dark')`
- Ensure all CSS variables use `:root.dark` / `.dark` selectors

### 3.5 Transition animations
- Add `cs-page-fade` transition to LoginView and SearchResultsView
- Ensure consistent enter/leave timing

### 3.6 Responsive fixes
- Tables: horizontal scroll container on small screens
- Sidebar: auto-collapse trigger point
- SearchResultsView: sidebar collapse at smaller breakpoint

### 3.7 ScansView dialogs
- Add proper scoped styles for dialog/drawer body (currently `<style scoped>` is empty)

---

## Phase 4 — Performance Optimization

### 4.1 ECharts singleton registration
- Move `use([...])` from `DashboardView.vue:19-27` to `main.ts` or `echarts-setup.ts`
- Register once at app bootstrap

### 4.2 Watch debounce
- AuditQueueView `watch` → add 300ms debounce to `fetchList()`
- Prevent rapid filter switches from triggering multiple API calls

### 4.3 v-memo for long lists
- VulnTable: add `v-memo` on table rows
- TicketsView: add `v-memo` on table rows

### 4.4 Monaco lazy loading
- Current implementation (`defineAsyncComponent` + `requestIdleCallback`) is optimal
- Keep as-is, no changes needed

---

## Implementation Order

```
Phase 1 (type/dead code) ─────────────────────┐
                                               │
Phase 2 (stores) ──────────────────────────────┤
                                               │ Sequential (bottom-up)
Phase 3 (UI/UX) ───────────────────────────────┤  within each phase,
                                               │  parallel within phase
Phase 4 (perf) ────────────────────────────────┘
```

Each phase's changes are independent within the phase and can be parallelized.
