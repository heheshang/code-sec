# 任务拆解 — E-S3-PDF 审计底稿 PDF 导出

> 基于 `spec.md`（E-S3-PDF）拆解的可执行任务清单。
> **总工时**: 50h

---

## 任务列表

### 任务 1：PDF 生成引擎集成 + 基础工具

- **优先级**: P1
- **估计工时**: 8h
- **涉及模块**: `backend/common/` + `backend/api/`
- **涉及文件**:
  - `backend/common/pom.xml` — 添加 openpdf 依赖
  - `backend/api/src/main/java/com/codesec/api/module/export/PdfGenerator.java` — PDF 生成基础类（新增）
  - `backend/api/src/main/resources/fonts/NotoSansCJK-Regular.ttf` — 中文字体嵌入（需确认 License）
  - `backend/api/src/main/java/com/codesec/api/module/export/ExportConfig.java` — 导出配置（新增）
- **验收标准**:
  - [ ] `mvn -pl common compile` 通过
  - [ ] `PdfGenerator` 可生成包含中文的空白 PDF
  - [ ] PDF 文件可被 Adobe Acrobat/Chrome 正确打开
- **依赖任务**: 无

---

### 任务 2：审计底稿模板

- **优先级**: P1
- **估计工时**: 12h
- **涉及模块**: `backend/api/`
- **涉及文件**:
  - `backend/api/src/main/java/com/codesec/api/module/export/AuditReportTemplate.java` — 底稿模板渲染（新增）
  - `backend/api/src/main/java/com/codesec/api/module/export/PdfStyles.java` — 样式定义（新增）
  - `backend/api/src/main/java/com/codesec/api/module/export/PdfPageDecorator.java` — 页眉/页脚/页码（新增）
- **验收标准**:
  - [ ] 模板包含完整 7 章节：页眉、基本信息、漏洞详情、代码片段、审计记录、修复建议、页脚
  - [ ] 中文渲染正确，无方框/乱码
  - [ ] 代码片段带行号，关键字加粗
  - [ ] 页码格式 "第 X 页 / 共 Y 页"
- **依赖任务**: T1

---

### 任务 3：导出 API

- **优先级**: P1
- **估计工时**: 16h
- **涉及模块**: `backend/api/`
- **涉及文件**:
  - `backend/api/src/main/java/com/codesec/api/module/export/ExportController.java` — 导出端点（新增）
  - `backend/api/src/main/java/com/codesec/api/module/export/ExportService.java` — 导出业务逻辑（新增）
  - `backend/api/src/main/java/com/codesec/api/module/export/ZipBuilder.java` — 批量 ZIP 打包（新增）
- **验收标准**:
  - [ ] `GET /api/v1/tickets/{id}/export` 返回 `Content-Type: application/pdf`
  - [ ] PDF 包含工单关联的完整审计记录
  - [ ] `POST /api/v1/tickets/export-batch` 返回 ZIP
  - [ ] 错误处理：不存在的工单返回 404
  - [ ] 权限控制：仅审计员可导出
- **依赖任务**: T2

---

### 任务 4：前端下载按钮

- **优先级**: P1
- **估计工时**: 8h
- **涉及模块**: `frontend/`
- **涉及文件**:
  - `frontend/src/api/export.ts` — 导出 API 客户端（新增）
  - `frontend/src/views/WorkbenchView.vue` — 添加 "导出 PDF" 按钮
  - `frontend/src/views/AuditQueueView.vue` — 添加批量导出按钮
  - `frontend/src/components/audit/ExportButton.vue` — 通用导出按钮组件（新增）
- **验收标准**:
  - [ ] WorkbenchView 右上角有导出按钮，点击下载工单 PDF
  - [ ] AuditQueueView 多选后显示批量导出按钮
  - [ ] 导出时显示 loading 状态
  - [ ] 导出成功浏览器自动保存文件
- **依赖任务**: T3

---

### 任务 5：测试 + 文档

- **优先级**: P1
- **估计工时**: 6h
- **涉及模块**: `backend/api/`
- **涉及文件**:
  - `backend/api/src/test/java/com/codesec/api/module/export/ExportControllerTest.java`（新增）
  - `backend/api/src/test/java/com/codesec/api/module/export/ExportServiceTest.java`（新增）
  - `backend/api/src/test/java/com/codesec/api/module/export/AuditReportTemplateTest.java`（新增）
- **验收标准**:
  - [ ] 导出 API 集成测试覆盖单个导出和批量导出
  - [ ] 模板测试验证 PDF 内容包含预期文本
  - [ ] `mvn -pl api test` 全部通过
- **依赖任务**: T1, T2, T3

---

## 排期

| 任务 | 优先级 | 工时 | 依赖 | 计划开始 |
|------|--------|------|------|----------|
| T1: PDF 引擎集成 | P1 | 8h | - | D4 |
| T2: 底稿模板 | P1 | 12h | T1 | D4 |
| T3: 导出 API | P1 | 16h | T2 | D5 |
| T4: 前端按钮 | P1 | 8h | T3 | D6 |
| T5: 测试 | P1 | 6h | T1,T2,T3 | D6 |
