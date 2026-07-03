# 前端架构补充设计

> **版本**: v1.0  
> **状态**: Draft  
> **关联**: PRD § 3.6 人工审计工作台, README.md frontend/ 目录  
> **当前状态**: Vue 3 + TypeScript + Vite + Element Plus + CodeMirror 6 + ECharts，基础 11 视图已完成

---

## 1. 设计目标

在当前前端基础上，补充以下关键架构设计：

1. **AI 审计专区** — AI 结论展示、交互、可信度表达
2. **专业代码查看器** — 大文件渲染、漏洞标记、代码 diff
3. **图谱可视化** — 污点传播链路、函数调用图的交互展示
4. **实时推送** — WebSocket 驱动的扫描状态/漏洞实时更新
5. **审计工作台增强** — 批量操作、快捷键体系、AI + 人工协同

---

## 2. 新增目录结构

```
frontend/src/
├── ai-audit/                          # [新增] AI 审计专区
│   ├── components/
│   │   ├── AiAuditPanel.vue           # AI 审计主面板
│   │   ├── AiConfidenceBadge.vue      # 可信度徽章
│   │   ├── AiVerdictTimeline.vue      # AI 决策链路时间线
│   │   ├── AiFixSuggestion.vue        # AI 修复建议展示
│   │   └── AiBatchActions.vue         # AI 批量操作
│   ├── composables/
│   │   └── useAiAudit.ts              # AI 审计逻辑封装
│   └── stores/
│       └── aiAuditStore.ts            # AI 审计状态
│
├── code-viewer/                       # [重构] 代码查看器
│   ├── CodeViewer.vue                 # 主组件
│   ├── CodeDiffViewer.vue             # 修复 diff 对比
│   ├── FileBreadcrumb.vue             # 文件路径面包屑
│   ├── LineMarker.vue                 # 漏洞行标记
│   └── composables/
│       ├── useVirtualScroll.ts        # 虚拟滚动
│       └── useCodeSelection.ts        # 代码选取
│
├── graph/                             # [新增] 图谱可视化
│   ├── CallGraphView.vue              # 函数调用图
│   ├── TaintFlowView.vue              # 污点传播流图
│   ├── GraphToolbar.vue               # 图工具栏（缩放/布局/导出）
│   └── composables/
│       └── useGraphInteraction.ts     # 图交互逻辑
│
├── workbench/                         # [重构] 审计工作台
│   ├── AuditWorkbench.vue             # 主布局（三栏）
│   ├── VulnListSidebar.vue            # 左侧：漏洞列表
│   ├── CodeMainPanel.vue             # 中间：代码展示
│   ├── ActionRightPanel.vue          # 右侧：操作面板
│   └── composables/
│       └── useWorkbenchLayout.ts      # 布局管理
│
├── realtime/                          # [新增] 实时更新
│   ├── composables/
│   │   └── useWebSocket.ts            # WebSocket 封装
│   └── components/
│       ├── ScanProgressBar.vue        # 扫描进度条
│       └── RealtimeNotification.vue   # 实时通知
│
├── search/                            # [增强] 全文搜索
│   └── ...                            # 已有，补充 AI 审计搜索
│
└── styles/
    ├── ai-audit.css                   # AI 审计相关样式
    ├── graph.css                      # 图谱相关样式
    └── code-viewer.css                # 代码查看器样式
```

---

## 3. AI 审计面板设计

### 3.1 交互布局

```
┌──────────────────────────────────────────────────────────┐
│  AI 审计结论                             置信度: ████▁ 80% │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  🔴 真实漏洞                                              │
│  ────────────────────────────────────────────────         │
│  类型: SQL 注入 (CWE-89)                                  │
│  严重度: HIGH                                             │
│                                                          │
│  判断依据:                                                │
│  userId 来自 @PathVariable，未经任何校验直接拼接 SQL       │
│  语句。框架未使用 MyBatis 参数化查询或 Spring Data JPA。   │
│                                                          │
│  污点链路:                                                │
│  [入口] getUser(@PathVariable userId)                      │
│     ↓ request.getParameter("userId")                      │
│  [传播] userDao.findById(userId)                          │
│     ↓ String sql = "SELECT * FROM users WHERE id = " + id │
│  [触发] stmt.executeQuery(sql)                          │
│                                                          │
│  ┌──────────────────────────────────────────────────┐     │
│  │  🤖 AI 修复建议                                    │     │
│  │  +---------------------------------------------- │     │
│  │  │ - String sql = "SELECT * FROM users WHERE │     │
│  │  │                 id = " + userId;            │     │
│  │  │ + String sql = "SELECT * FROM users WHERE │     │
│  │  │                 id = ?";                    │     │
│  │  │ + PreparedStatement ps =                    │     │
│  │  │     conn.prepareStatement(sql);              │     │
│  │  │ + ps.setLong(1, userId);                    │     │
│  │  └──────────────────────────────────────────────┘     │
│  │  [复制] [应用补丁]                                      │
│  └──────────────────────────────────────────────────┘     │
│                                                          │
│  ┌──────────────────────────────────────────────────┐     │
│  │  📋 参考依据 (RAG)                                 │     │
│  │  • OWASP: SQL Injection Prevention Cheat Sheet    │     │
│  │  • 历史漏洞: 2024-03-15 支付服务类似漏洞            │     │
│  │  • 修复方案参考: PreparedStatement 参数化查询       │     │
│  └──────────────────────────────────────────────────┘     │
│                                                          │
├──────────────────────────────────────────────────────────┤
│  [✅ 确认为漏洞]  [❌ 标记误报]  [❓ 存疑待人工]  [编辑]    │
└──────────────────────────────────────────────────────────┘
```

### 3.2 组件树

```vue
<template>
  <div class="ai-audit-panel">
    <!-- 头部：结论摘要 -->
    <AiVerdictHeader
      :verdict="verdict"
      :confidence="confidence"
    />
    <!-- 主体：判断依据 + 污点链路 -->
    <VerdictReason :reason="reason" />
    <TaintFlowChain :chain="taintChain" />
    <!-- 修复建议（可折叠） -->
    <AiFixSuggestion
      v-if="patch"
      :patch="patch"
      @copy="copyPatch"
      @apply="applyPatch"
    />
    <!-- 参考依据（RAG） -->
    <ReferenceSources :sources="references" />
    <!-- 操作按钮 -->
    <div class="action-bar">
      <el-button type="primary" @click="confirmVuln">确认为漏洞</el-button>
      <el-button @click="markFalsePositive">标记误报</el-button>
      <el-button @click="markUncertain">存疑待人工</el-button>
    </div>
  </div>
</template>
```

---

## 4. 代码查看器设计

### 4.1 技术选型

| 组件 | 选型 | 理由 |
|------|------|------|
| 编辑器核心 | **CodeMirror 6** | 轻量（Monaco 的 1/5）、虚拟滚动原生支持、多语言语法 |
| 语法高亮 | `@codemirror/lang-java/go/python/javascript` | CM6 官方语言包 |
| 主题 | 自定义 + `@codemirror/theme-one-dark` | 暗色主题适合代码审计 |
| 行标记 | `StateEffect` 自定义 | 漏洞行高亮 + 悬浮信息 |
| 代码 diff | Merchantile diff 算法 + 并排渲染 | 修复前后对比 |

### 4.2 大文件虚拟滚动

```typescript
// useVirtualScroll.ts — 核心逻辑
export function useVirtualScroll(options: {
  containerHeight: number;   // 容器高度
  lineHeight: number;        // 行高（默认 20px）
  totalLines: number;        // 总行数
  buffer: number;            // 缓冲区行数（默认 20）
}) {
  const visibleRange = ref({ start: 0, end: 0 });
  const scrollTop = ref(0);

  function onScroll(event: Event) {
    const target = event.target as HTMLElement;
    scrollTop.value = target.scrollTop;
    const start = Math.floor(target.scrollTop / options.lineHeight);
    visibleRange.value = {
      start: Math.max(0, start - options.buffer),
      end: Math.min(options.totalLines, start + visibleCount() + options.buffer),
    };
  }

  function visibleCount() {
    return Math.ceil(options.containerHeight / options.lineHeight);
  }

  return { visibleRange, scrollTop, onScroll };
}
```

### 4.3 漏洞行标记

```typescript
// LineMarker 实现：在 CodeMirror 6 中标记漏洞行
import { StateEffect, StateField } from '@codemirror/state';
import { Decoration, DecorationSet, EditorView } from '@codemirror/view';

// 定义标记效果
const addMarker = StateEffect.define<{ line: number; severity: string }>();

// 标记状态字段
const markerField = StateField.define<DecorationSet>({
  create() { return Decoration.none; },
  update(markers, tr) {
    for (const effect of tr.effects) {
      if (effect.is(addMarker)) {
        const { line, severity } = effect.value;
        const deco = Decoration.line({
          class: `vuln-line vuln-${severity}`,
          attributes: { 'data-vuln-line': String(line) },
        });
        markers = markers.update({
          add: [{ from: tr.view.lineBlockAt(line).from, value: deco }],
        });
      }
    }
    return markers.map(tr.changes);
  },
  provide: f => EditorView.decorations.from(f),
});
```

---

## 5. 图谱可视化

### 5.1 技术选型

| 组件 | 选型 | 理由 |
|------|------|------|
| 图渲染引擎 | **@vue-flow/core** | react-flow 的 Vue 移植，DAG 布局原生支持 |
| 布局算法 | **dagre** | 层次化布局，适合调用链图 |
| 节点样式 | 自定义 Vue 组件 | 可交互、可下钻、可展开折叠 |
| 交互 | 缩放+拖拽+点击高亮 | @vue-flow 内置 |

### 5.2 调用链图交互设计

```
┌──────────────────────────────────────────────────────────┐
│  污点传播链路                             [缩放] [适应] │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  ┌──────────┐                                            │
│  │ getUser  │─── 入口 ──────────────────────────         │
│  │ @GetMapping│   🟢 用户可控: userId(@PathVariable)      │
│  └────┬─────┘                                            │
│       │                                                  │
│       ▼                                                  │
│  ┌──────────┐                                            │
│  │validateId│─── 校验层 ────────────────────────          │
│  │ @param   │   🔴 未做任何校验                          │
│  └────┬─────┘                                            │
│       │                                                  │
│       ▼                                                  │
│  ┌──────────┐                                            │
│  │findById  │─── 数据层 ────────────────────────          │
│  │ UserDao  │   ⚠️ 直接拼接 SQL                           │
│  └────┬─────┘                                            │
│       │                                                  │
│       ▼                                                  │
│  ┌──────────┐                                            │
│  │execute   │─── 触发点 ────────────────────────          │
│  │Query     │   🔴 可控数据进入 SQL 执行                  │
│  └──────────┘                                            │
│                                                          │
├──────────────────────────────────────────────────────────┤
│  🟢 可控路径  🔴 不可控路径  ⚠️ 部分可控                   │
└──────────────────────────────────────────────────────────┘
```

---

## 6. WebSocket 实时推送

### 6.1 封装

```typescript
// useWebSocket.ts
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export function useWebSocket() {
  const client = new Client({
    webSocketFactory: () => new SockJS('/ws'),
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
  });

  function subscribe(destination: string, callback: (msg: any) => void) {
    return client.subscribe(destination, (message) => {
      callback(JSON.parse(message.body));
    });
  }

  // 预定义订阅主题
  const topics = {
    scanProgress: (scanId: string) => `/topic/scan/${scanId}/progress`,
    newVuln: (projectId: string) => `/topic/project/${projectId}/vuln`,
    scanComplete: (scanId: string) => `/topic/scan/${scanId}/complete`,
    ticketUpdate: (ticketId: string) => `/topic/ticket/${ticketId}`,
  };

  return { client, subscribe, topics };
}
```

---

## 7. 状态管理扩展

```typescript
// stores/aiAuditStore.ts — Pinia store
export const useAiAuditStore = defineStore('aiAudit', () => {
  // 状态
  const aiResults = ref<Map<string, AiVerdict>>(new Map());
  const isAnalyzing = ref(false);
  const analysisQueue = ref<string[]>([]);

  // 操作
  async function analyzeVuln(vulnId: string, snippet: string) {
    isAnalyzing.value = true;
    analysisQueue.value.push(vulnId);
    try {
      const result = await api.post('/api/v1/ai/analyze', { vulnId, snippet });
      aiResults.value.set(vulnId, result.data);
      return result.data;
    } finally {
      const idx = analysisQueue.value.indexOf(vulnId);
      if (idx >= 0) analysisQueue.value.splice(idx, 1);
      isAnalyzing.value = analysisQueue.value.length > 0;
    }
  }

  // 批量 AI 审计
  async function batchAnalyze(vulnIds: string[]) {
    // 分批发送，每批 10 个
    const batchSize = 10;
    for (let i = 0; i < vulnIds.length; i += batchSize) {
      const batch = vulnIds.slice(i, i + batchSize);
      await Promise.all(batch.map(id => analyzeVuln(id, '')));
    }
  }

  return { aiResults, isAnalyzing, analyzeVuln, batchAnalyze };
});
```

---

## 8. 新增依赖

```json
{
  "dependencies": {
    "@vue-flow/core": "^1.0.0",
    "@vue-flow/minimap": "^1.0.0",
    "@vue-flow/controls": "^1.0.0",
    "@stomp/stompjs": "^7.0.0",
    "sockjs-client": "^1.6.0",
    "@codemirror/lang-java": "^6.0.0",
    "@codemirror/lang-go": "^6.0.0",
    "@codemirror/lang-python": "^6.0.0",
    "@codemirror/lang-javascript": "^6.0.0",
    "@codemirror/theme-one-dark": "^6.0.0"
  }
}
```

---

## 9. 性能目标

| 指标 | 目标 | 方案 |
|------|------|------|
| 大文件加载（万行+） | < 1s | CodeMirror 6 虚拟滚动 |
| 代码 diff 渲染 | < 500ms | 增量 diff 算法 |
| 图渲染（1000 节点） | < 2s | @vue-flow 虚拟化 |
| WebSocket 推送延迟 | < 1s | STOMP + 心跳保活 |
| AI 结论展示延迟 | < 500ms（收到结果后）| 预渲染骨架 |
