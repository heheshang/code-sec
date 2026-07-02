# Ant Design Vue → Element Plus 迁移实施计划

> **For Sisyphus:** 全量迁移 31 个文件。按阶段执行，每阶段独立可验证。

**目标：** 将前端 UI 框架从 ant-design-vue v4 替换为 element-plus，修复 /audit/2 交互问题

**设计文档：** `docs/plans/2026-07-02-antd-to-element-plus-migration.md`

---

### Task 1: 安装/卸载依赖 + main.ts

**Files:**
- Modify: `frontend/package.json`
- Modify: `frontend/src/main.ts`

**操作：**
```bash
cd frontend
npm uninstall ant-design-vue @ant-design/icons-vue
npm install element-plus @element-plus/icons-vue
```

**更新 main.ts：**
移除 `import Antd from 'ant-design-vue'` 和 `app.use(Antd)`
添加 `import ElementPlus from 'element-plus'` + `import 'element-plus/dist/index.css'` + `app.use(ElementPlus)`

---

### Task 2: App.vue — 移除 ConfigProvider

**Files:**
- Modify: `frontend/src/App.vue`

移除 `ConfigProvider` 包裹层，改为普通 `div` 或 fragment。
主题色通过 CSS 变量由 global.css 控制。
保留 `useUiStore().theme` 逻辑。

---

### Task 3: global.css — 重写 ant-* 样式

**Files:**
- Modify: `frontend/src/styles/global.css`

移除所有 `.ant-layout-*`、`.ant-layout-sider`、`.ant-layout-header`、`.ant-layout-content`、`.ant-layout-footer` 覆盖样式。
添加 Element Plus CSS 变量覆盖（`--el-color-primary` 等）。
保留非 ant 的纯 CSS（滚动条、severity tag、dashboard hero 等）。

---

### Task 4: AppLayout.vue — 布局组件迁移

**Files:**
- Modify: `frontend/src/components/layout/AppLayout.vue`

关键映射：
- `import { Layout } from 'ant-design-vue'` → 移除
- `<Layout>` → `<el-container>`
- `<Layout.Sider>` → `<el-aside>`
- `<Layout.Header>` → `<el-header>`
- `<Layout.Content>` → `<el-main>`
- `<Layout.Footer>` → `<el-footer>`
- 保留 `<transition>` 和 `<router-view>`
- scoped CSS 中的 `.cs-app` 类保留，调整引用

---

### Task 5: SidebarNav.vue — 侧边栏导航迁移

**Files:**
- Modify: `frontend/src/components/layout/SidebarNav.vue`

关键改动：
- `import { Menu } from 'ant-design-vue'` → 移除
- `import { h } from 'vue'` 不再需要（手动写 el-menu-item）
- 图标从 `@ant-design/icons-vue` 改为 `@element-plus/icons-vue`
- `<Menu>` → `<el-menu>`：`mode="inline"` → `mode="vertical"`；`:selected-keys` → `:default-active`；`:inline-collapsed` → `:collapse`；`items` prop → 手动 `<el-menu-item>` 子元素
- `ui.sidebarCollapsed` 保持，传给 el-menu 的 collapse
- `handleSelect` 改为使用 Element Plus Menu 的 select 事件（参数为 index: string）
- 底部用户图标同步迁移

---

### Task 6: TopBar.vue — 顶栏迁移

**Files:**
- Modify: `frontend/src/components/layout/TopBar.vue`

将 Button、Dropdown、Badge、Space 等替换为 Element Plus 对应组件。
图标替换为 `@element-plus/icons-vue`。

---

### Task 7: 通用组件迁移（PageHeader, EmptyState, StatCard）

**Files:**
- Modify: `frontend/src/components/common/PageHeader.vue`
- Modify: `frontend/src/components/common/EmptyState.vue`
- Modify: `frontend/src/components/common/StatCard.vue`

PageHeader: Breadcrumb → el-breadcrumb, Typography.Text → span, Typography.Title → h3
EmptyState: Empty → el-empty
StatCard: Card → el-card, Skeleton → el-skeleton

---

### Task 8: 视图页面迁移（10 views）

**Files:**
- `frontend/src/views/DashboardView.vue`
- `frontend/src/views/AuditQueueView.vue`
- `frontend/src/views/WorkbenchView.vue`
- `frontend/src/views/LoginView.vue`
- `frontend/src/views/ScansView.vue`
- `frontend/src/views/TicketsView.vue`
- `frontend/src/views/ReposView.vue`
- `frontend/src/views/RulesView.vue`
- `frontend/src/views/ReportsView.vue`
- `frontend/src/views/SettingsView.vue`
- `frontend/src/views/SearchResultsView.vue`

每个文件批量替换：
- Button → el-button | Card → el-card | Tag → el-tag
- Row/Col → el-row/el-col | Space → el-space / flex
- Form/Form.Item → el-form/el-form-item | Input → el-input | Select → el-select
- Table → el-table + el-table-column | DatePicker → el-date-picker
- Modal → el-dialog | Collapse → el-collapse
- Spin → v-loading (directive) | Empty → el-empty
- message → ElMessage (import from element-plus)
- Typography.Text → span, Typography.Title → h1/h2/h3, Typography.Paragraph → p
- Divider → el-divider | Skeleton → el-skeleton
- 图标 → @element-plus/icons-vue

---

### Task 9: 功能组件迁移（13 components）

**Files:**
- `frontend/src/components/audit/AuditActionPanel.vue`
- `frontend/src/components/audit/AuditHistoryTimeline.vue`
- `frontend/src/components/audit/ExploitConditionForm.vue`
- `frontend/src/components/audit/PocUploader.vue`
- `frontend/src/components/audit/FixSnippetEditor.vue`
- `frontend/src/components/code/VulnLineMarker.vue`
- `frontend/src/components/code/CodeViewer.vue`
- `frontend/src/components/vuln/VulnFilters.vue`
- `frontend/src/components/vuln/VulnTable.vue`
- `frontend/src/components/vuln/ExploitabilityBadge.vue`
- `frontend/src/components/search/GlobalSearch.vue`
- `frontend/src/components/search/SearchFilters.vue`
- `frontend/src/components/search/VulnSearchResultItem.vue`
- `frontend/src/components/search/SnippetSearchResultItem.vue`

同上批量替换规则。

---

### Task 10: 清理验证

1. Grep 检查残留 `from 'ant-design-vue'` 和 `from '@ant-design/icons-vue'` 引用
2. `npx vue-tsc --noEmit` 类型检查
3. `npm run build` 构建验证
4. 手动测试 Dashboard → Audit queue → Workbench 流程
