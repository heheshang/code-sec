# Frontend Comprehensive Optimization — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Type safety, store refactoring, UI/UX improvements, and performance optimization for the code-sec frontend

**Architecture:** Bottom-up serial — Phase 1 (types/dead code) → Phase 2 (stores) → Phase 3 (UI/UX) → Phase 4 (performance)

**Tech Stack:** Vue 3 + TypeScript + Vite + Element Plus + Pinia + ECharts + Monaco Editor

**Working directory:** `/Users/ssk/Documents/project/personal/ai/code-sec/frontend`

---

## Phase 1 — Type Safety & Dead Code Elimination

### Task 1.1: Fix `as any` type casts in statusType/severityType maps

**Files:**
- Modify: `src/views/ScansView.vue`
- Modify: `src/views/TicketsView.vue`
- Modify: `src/views/ReposView.vue`
- Modify: `src/views/RulesView.vue`
- Modify: `src/views/ReportsView.vue`
- Modify: `src/components/vuln/VulnTable.vue`

**Step 1: Create strict type for status map values**

All `statusType` / `statusColorMap` / `severityType` / `frequencyType` objects use string index signatures with `as any` casts. Replace with properly typed maps.

Pattern for each file:
```typescript
// BEFORE
const statusType: Record<string, string> = {
  pending: '',
  running: 'primary',
  completed: 'success',
  failed: 'danger',
  cancelled: 'warning',
}

// AFTER
type StatusType = '' | 'primary' | 'success' | 'danger' | 'warning' | 'info'
const statusType: Record<string, StatusType> = {
  pending: '',
  running: 'primary',
  completed: 'success',
  failed: 'danger',
  cancelled: 'warning',
}
```

Then remove `as any` casts in templates — change `(statusType[row.status] as any) ?? ''` to `statusType[row.status] ?? ''`.

**Step 2: Apply to each file**

1. `ScansView.vue` — `statusType` map (line 21-27), remove `as any` on line 119
2. `TicketsView.vue` — `statusType` map (line 12-21), remove `as any` on lines 92, 153
3. `ReposView.vue` — `statusType` map (line 101-105), remove `as any` on line 146
4. `RulesView.vue` — `severityType` map (line 52-54), remove `as any` on lines 172, 224
5. `ReportsView.vue` — `frequencyType` map (line 41-45), remove `as any` on line 71
6. `VulnTable.vue` — `statusColorMap` map (line 18-27), remove `as any` on line 92

**Step 3: Verify**
```bash
npx vue-tsc --noEmit
```
Expected: No type errors.

---

### Task 1.2: Fix `catch (e: any)` patterns

**Files:**
- Modify: `src/views/ScansView.vue`
- Modify: `src/views/ReposView.vue`
- Modify: `src/views/RulesView.vue`
- Create: `src/utils/error.ts`

**Step 1: Create error utility**

```typescript
// src/utils/error.ts
export function errMsg(e: unknown): string {
  if (e instanceof Error) return e.message
  if (typeof e === 'string') return e
  if (e && typeof e === 'object' && 'message' in e && typeof (e as Record<string, unknown>).message === 'string') {
    return (e as Record<string, unknown>).message as string
  }
  return 'An unexpected error occurred'
}
```

**Step 2: Replace `catch (e: any)` in RulesView.vue**

```typescript
// BEFORE
} catch (e: any) {
  ElMessage.error(e.message || 'Failed to load rules')
}

// AFTER  
import { errMsg } from '@/utils/error'
// ...
} catch (e: unknown) {
  ElMessage.error(errMsg(e))
}
```

Affected lines in RulesView: 67-68, 80-81, 92-93, 114-115, 126-127, 136-137

**Step 3: Replace in ReposView.vue**

```typescript
// BEFORE
} catch {
  /* validation failed */
}

// Keep empty catch for form validation — this is intentional
```

Line 73-75: Keep as-is (form validation failure, no message needed)

**Step 4: Replace in ScansView.vue**

Line 33: `catch { /* ignore */ }` — Intentional, keep as-is
Line 53-54, 63-64, 73-74: Replace `catch { ElMessage.error(...) }` with `catch (e: unknown) { ElMessage.error(errMsg(e)) }`

**Step 5: Verify**
```bash
npx vue-tsc --noEmit
```

---

### Task 1.3: Remove dead code

**Files:**
- Modify: `src/views/ReposView.vue`
- Modify: `src/views/AuditQueueView.vue`
- Modify: `src/components/vuln/VulnTable.vue`
- Modify: `src/stores/vuln.ts`

**Step 1: ReposView.vue — Remove `columns` and invalid `:pagination` prop**

Remove lines 11-19 (the `columns` array definition).
Remove lines 123-127 (`:pagination="{...}"` prop from `<el-table>`).

**Step 2: AuditQueueView.vue — Simplify `showBulkDrawer`**

```typescript
// BEFORE
const showBulkDrawer = computed({
  get: () => false,
  set: () => undefined,
})

// AFTER
const showBulkDrawer = ref(false)
```

**Step 3: VulnTable.vue — Remove dead `projectNameMap`**

Remove lines 14-16 (the `projectNameMap` computed that always returns `{}`).

**Step 4: vuln.ts — Remove `void` statements**

Remove lines 166-167:
```typescript
void SEVERITY_ORDER
void vulnFromApi
```

**Step 5: Verify**
```bash
npx vue-tsc --noEmit
```

---

### Task 1.4: Unify dark mode mechanism

**Files:**
- Modify: `src/stores/ui.ts`
- Modify: `src/styles/global.css`
- Modify: `src/styles/variables.css`

**Step 1: ui.ts — Switch from `data-cs-theme` to `.dark` class**

```typescript
// BEFORE
document.documentElement.setAttribute('data-cs-theme', t)

// AFTER (in both setTheme and init)
if (t === 'dark') {
  document.documentElement.classList.add('dark')
} else {
  document.documentElement.classList.remove('dark')
}
```

**Step 2: variables.css — Change selectors**

```css
/* BEFORE */
[data-cs-theme='dark'] {
  /* ... */
}

/* AFTER */
:root.dark {
  /* ... */
}
```

**Step 3: global.css — Remove data-cs-theme references**

Lines 74-85: The `.dark` class overrides are fine (they target Element Plus variables), just ensure they're correct.

**Step 4: Verify**
```bash
npx vue-tsc --noEmit
```

---

### Task 1.5: Miscellaneous fixes

**Files:**
- Modify: `src/views/LoginView.vue`
- Modify: `src/views/ReportsView.vue`
- Modify: `src/views/SettingsView.vue`

**Step 1: LoginView.vue — Remove hardcoded demo credentials from template**

```vue
<!-- BEFORE -->
<span class="cs-login__hint">Demo: admin / admin123</span>

<!-- AFTER -->
<span class="cs-login__hint">Enter your credentials to sign in</span>
```

**Step 2: ReportsView.vue — Replace mock setTimeout with proper API integration**

The current mock generates a report and simulates 900ms delay. Replace with:
```typescript
async function handleGenerate(t: ReportTemplate): Promise<void> {
  generatingId.value = t.id
  try {
    await http.post(`/reports/${t.id}/generate`)
    t.lastGeneratedAt = new Date().toISOString()
    ElMessage.success(`${t.name} is being generated. Check your email in ~2 minutes.`)
  } catch (e: unknown) {
    ElMessage.error(errMsg(e))
  } finally {
    generatingId.value = null
  }
}
```

**Step 3: SettingsView.vue — Add comment for mock data**

Move inline mock to documented comment at top of `<script>`.

**Step 4: Verify**
```bash
npx vue-tsc --noEmit
```

---

## Phase 2 — Store Layer Refactoring

### Task 2.1: Create `composables/useCrudStore.ts`

**Files:**
- Create: `src/composables/useCrudStore.ts`

```typescript
import { ref } from 'vue'
import { http } from '@/api/client'

interface CrudStoreOptions<T> {
  baseUrl: string
  pageSize?: number
  transform?: (raw: unknown) => T
}

export function useCrudStore<T extends { id: number | string }>(opts: CrudStoreOptions<T>) {
  const items = ref<T[]>([])
  const total = ref(0)
  const page = ref(1)
  const pageSize = ref(opts.pageSize ?? 20)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchList(extraParams?: Record<string, unknown>): Promise<void> {
    loading.value = true
    error.value = null
    try {
      const params: Record<string, unknown> = { page: page.value, size: pageSize.value, ...extraParams }
      const resp = await http.get(opts.baseUrl, { params })
      items.value = (resp.data.items ?? []).map((raw: unknown) =>
        opts.transform ? opts.transform(raw) : raw
      ) as T[]
      total.value = resp.data.total
      page.value = resp.data.page
      pageSize.value = resp.data.size ?? resp.data.pageSize ?? pageSize.value
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to load'
    } finally {
      loading.value = false
    }
  }

  async function getById(id: number | string): Promise<T> {
    const resp = await http.get<T>(`${opts.baseUrl}/${id}`)
    return resp.data
  }

  async function create(payload: Partial<T>): Promise<T> {
    const resp = await http.post<T>(opts.baseUrl, payload)
    await fetchList()
    return resp.data
  }

  async function update(id: number | string, payload: Partial<T>): Promise<T> {
    const resp = await http.put<T>(`${opts.baseUrl}/${id}`, payload)
    await fetchList()
    return resp.data
  }

  async function remove(id: number | string): Promise<void> {
    await http.delete(`${opts.baseUrl}/${id}`)
    await fetchList()
  }

  function setPage(p: number): void { page.value = p }
  function setPageSize(s: number): void { pageSize.value = s; page.value = 1 }

  return {
    items, total, page, pageSize, loading, error,
    fetchList, getById, create, update, remove, setPage, setPageSize,
  } as const
}
```

---

### Task 2.2: Refactor scan.ts

**Files:**
- Modify: `src/stores/scan.ts`

Use `useCrudStore` for common CRUD, keep `cancel()` and `setRepoId()`:

```typescript
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { http } from '@/api/client'
import { useCrudStore } from '@/composables/useCrudStore'
import type { ScanListItem, ScanTaskResponse, ScanCreateRequest } from '@/types/scan'

export const useScanStore = defineStore('scan', () => {
  const crud = useCrudStore<ScanListItem>({ baseUrl: '/scans' })
  const repoId = ref<number | null>(null)

  // Override fetchList to include repoId filter
  async function fetchList(rId?: number): Promise<void> {
    if (rId !== undefined) repoId.value = rId
    await crud.fetchList(repoId.value ? { repoId: repoId.value } : undefined)
  }

  async function getScanById(id: number): Promise<ScanTaskResponse> {
    const resp = await http.get<ScanTaskResponse>(`/scans/${id}`)
    return resp.data
  }

  async function createScan(req: ScanCreateRequest): Promise<{ scanId: number }> {
    const resp = await http.post<{ scanId: number }>('/scans', req)
    return resp.data
  }

  async function cancel(id: number): Promise<void> {
    await http.delete(`/scans/${id}`)
    await fetchList()
  }

  function setRepoId(rId: number | null): void {
    repoId.value = rId
    crud.setPage(1)
  }

  return {
    ...crud,
    repoId,
    fetchList,
    getScanById,
    createScan,
    cancel,
    setRepoId,
  }
})
```

---

### Task 2.3: Refactor ticket.ts

**Files:**
- Modify: `src/stores/ticket.ts`

Use `useCrudStore` for common CRUD, keep `transition()`, `assign()`, `getHistory()`, `setStatusFilter()`.

---

### Task 2.4: Refactor repo.ts

**Files:**
- Modify: `src/stores/repo.ts`

Use `useCrudStore`, keep `testConnection()`.

---

## Phase 3 — UI/UX Optimization

### Task 3.1: Create reusable EmptyState component

**Files:**
- Modify: `src/components/common/EmptyState.vue` (already exists)

Enhance existing EmptyState.vue with configurable props: `imageSize`, `description`, `action` slot.

---

### Task 3.2: Create Skeleton components

**Files:**
- Create: `src/components/common/SkeletonTable.vue`
- Create: `src/components/common/SkeletonCard.vue`

SkeletonTable: rows × columns table placeholder with animated pulse.
SkeletonCard: card with header/body placeholder.

---

### Task 3.3: Add global loading progress bar

**Files:**
- Modify: `src/router/index.ts`

Add route transition progress indicator:
```typescript
const loadingCount = ref(0)

router.beforeEach((_to, _from, next) => {
  loadingCount.value++
  // Show progress bar
  next()
})

router.afterEach(() => {
  loadingCount.value--
  if (loadingCount.value <= 0) {
    loadingCount.value = 0
    // Hide progress bar
  }
})
```

---

### Task 3.4: Apply transitions to remaining views

**Files:**
- Modify: `src/views/LoginView.vue`
- Modify: `src/views/SearchResultsView.vue`
- Modify: `src/App.vue` (if needed)

Wrap router-view content with `<transition name="cs-page-fade" mode="out-in">`.

---

### Task 3.5: Responsive fixes

**Files:**
- Modify: `src/views/ScansView.vue`
- Modify: `src/components/vuln/VulnTable.vue`
- Modify: `src/components/layout/AppLayout.vue`

Add horizontal scroll for tables on mobile, and adjust sidebar breakpoints.

---

### Task 3.6: ScansView dialog styles

**Files:**
- Modify: `src/views/ScansView.vue`

Add proper scoped styles for the dialog and drawer bodies (currently `<style scoped>` is empty).

---

## Phase 4 — Performance Optimization

### Task 4.1: ECharts singleton registration

**Files:**
- Create: `src/echarts-setup.ts`
- Modify: `src/main.ts`
- Modify: `src/views/DashboardView.vue`

Move all ECharts `use()` calls from DashboardView to `main.ts` or a dedicated `echarts-setup.ts`.

---

### Task 4.2: Watch debounce for AuditQueueView

**Files:**
- Modify: `src/views/AuditQueueView.vue`

Add 300ms debounce to the watch that triggers `fetchList()`.

---

### Task 4.3: v-memo for table rows

**Files:**
- Modify: `src/components/vuln/VulnTable.vue`
- Modify: `src/views/TicketsView.vue`

Add `v-memo="[row]"` on `<el-table-column>` or table row elements.
