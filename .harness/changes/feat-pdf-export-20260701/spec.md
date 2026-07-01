# 需求规格说明书 — E-S3-PDF 审计底稿 PDF 导出

> **Epic ID**: E-S3-PDF
> **Epic 名称**: 审计底稿 PDF 导出
> **变更目录**: `.harness/changes/feat-pdf-export-20260701/`
> **创建日期**: 2026-07-01
> **对应 Sprint**: Sprint 3（M1 第三冲刺）
> **总工时**: 50h
> **优先级**: P1
> **关联文档**:
> - 冲刺合同：`.harness/changes/sprint-3/contract.md` § 5.1
> - 审计模块：`backend/api/src/main/java/com/codesec/api/module/audit/`
> - 漏洞工单：`backend/api/src/main/java/com/codesec/api/module/ticket/`
> - 前端工单页：`frontend/src/views/WorkbenchView.vue`

---

## 1. 背景

安全审计员完成审计后，需要将审计结果导出为 PDF 底稿，用于：
1. **合规归档**：监管要求审计记录以不可篡改的格式保存
2. **外部沟通**：发送给开发团队/安全负责人确认
3. **纸质签字**：部分企业内部流程要求纸质审计底稿

当前系统没有导出功能，审计数据只能在平台内查看。

---

## 2. 需求描述

### 2.1 范围与边界

**范围内**：
- PDF 生成引擎集成（OpenPDF，纯 Java，Apache License）
- 审计底稿模板设计（标准安全审计报告格式）
- 导出 API（单个工单导出 + 批量导出）
- 前端下载按钮（WorkbenchView + AuditQueueView）
- 中文字体支持（Noto Sans CJK）
- 页眉/页脚/页码/水印

**范围外**：
- ❌ 定时自动导出 — Sprint 4
- ❌ 自定义模板 — M2
- ❌ HTML→PDF 方案（wkhtmltopdf）— 增加运维复杂度
- ❌ Excel/CSV 导出 — 独立需求

### 2.2 交付物清单

| # | 交付物 | 工时 |
|---|--------|------|
| T1 | PDF 生成引擎集成 + 基础工具 | 8h |
| T2 | 审计底稿模板设计 + 实现 | 12h |
| T3 | 导出 API（单个+批量） | 16h |
| T4 | 前端下载按钮 | 8h |
| T5 | 测试 + 文档 | 6h |

---

## 3. 技术方案

### 3.1 PDF 引擎选型

**选择 OpenPDF**（`com.github.librepdf:openpdf`）：
- 纯 Java，零原生依赖
- Apache License（兼容项目许可证）
- API 类似 iText 5，社区活跃
- 支持中文字体嵌入

### 3.2 审计底稿模板

PDF 包含以下章节：

1. **页眉**：code-sec 审计报告 + 生成时间
2. **基本信息**：漏洞ID、项目名称、文件路径、发现时间
3. **漏洞详情**：严重程度、CWE、利用性评分
4. **代码片段**：带行号源码 + 高亮标记
5. **审计记录**：审计员意见、标记状态、复测结果
6. **修复建议**：修复方案 + 参考链接
7. **页脚**：页码 + 密级标识

### 3.3 后端 API

| Method | Path | 说明 |
|--------|------|------|
| `GET` | `/api/v1/tickets/{id}/export` | 单个工单 PDF 导出 |
| `POST` | `/api/v1/tickets/export-batch` | 批量工单 PDF 导出（返回 ZIP） |

### 3.4 前端集成

- WorkbenchView：工单详情页添加 "导出 PDF" 按钮（右上角操作栏）
- AuditQueueView：列表添加批量导出按钮（选择多个工单 → 批量导出）
- 导出过程中显示 loading 状态
- 下载完成后浏览器自动保存

---

## 4. 验收标准

- [ ] `GET /api/v1/tickets/{id}/export` 返回 valid PDF（Content-Type: application/pdf）
- [ ] PDF 包含完整底稿：漏洞信息、代码片段、审计记录、修复建议
- [ ] 中文内容正确显示（Noto Sans CJK 字体嵌入）
- [ ] 批量导出返回 ZIP（含多个 PDF）
- [ ] 前端 WorkbenchView 有导出按钮，点击触发下载
- [ ] 前端 AuditQueueView 支持批量导出
- [ ] `mvn -pl api test` 全部通过
