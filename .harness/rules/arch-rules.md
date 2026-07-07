# 架构规则

> 工程结构约束与分层架构约定。这些是不随需求变化的稳定约束（Invariant Constraints）。

## 模块结构

```
price-center/
├── app/              # 接入层 — RPC Provider / Controller
│   ├── controller/   # REST 接口
│   ├── provider/     # HSF RPC Provider
│   └── validator/    # 参数校验
├── web/              # 展现层 — 管理后台
│   ├── controller/   # 后台接口
│   └── vo/           # 视图对象
├── core/             # 核心业务层
│   ├── service/      # 业务服务接口
│   ├── service/impl/ # 业务服务实现
│   ├── domain/       # 领域模型
│   ├── manager/      # 业务编排
│   └── flow/         # LiteFlow 流程组件
├── integration/      # 集成层
│   ├── rpc/          # 外部 RPC 调用
│   ├── cache/        # 缓存操作 Tair/Redis
│   └── mq/           # 消息队列
├── common/           # 公共层
│   ├── util/         # 工具类
│   ├── constant/     # 常量定义
│   └── exception/    # 异常定义
├── dal/              # 数据访问层
│   ├── mapper/       # MyBatis Mapper
│   ├── model/        # 数据模型（DO）
│   └── config/       # 数据源配置
└── bootstrap/        # 启动层
    └── config/       # 应用配置
```

## 分层依赖规则

```
app → core → integration
  ↘         ↘
   web → common → dal
```

- **上层可依赖下层，下层不可依赖上层**
- **common 可被任意层依赖，但 common 不可依赖其他模块**
- **app 和 web 层互不依赖**
- **dal 层不可依赖 core 和 integration**

## 核心架构原则

### 1. RPC 接口设计
- RPC 接口必须定义 interface + DTO，放在独立的 API 模块
- 接口必须指定 version 和 timeout
- DTO 必须实现 Serializable
- 返回值统一使用 Result<T> 包裹

### 2. 流程编排
- LiteFlow 组件必须委托 Service 层处理，组件内不写大段业务逻辑
- 组件职责单一，一个组件只做一件事
- 流程配置（XML）统一放在 `core/flow/config/` 目录

### 3. 配置中心
- Diamond 配置变更必须有对应的监听器
- 动态配置必须考虑默认值，防止配置中心不可用时系统异常
- 敏感配置（密码、密钥）禁止明文存储在配置中心

### 4. 缓存设计
- 缓存 Key 必须包含业务前缀，避免冲突
- 缓存更新必须考虑一致性问题（先更新 DB 再删除缓存）
- 批量操作必须注意缓存逐出的性能影响

### 5. 异常处理
- 业务异常继承 BaseBizException，包含错误码和错误消息
- 所有外部 RPC 调用必须有 try-catch 和降级处理
- 禁止在 catch 中吞掉异常（至少打印日志）

### 6. 国际化
- 用户侧展示信息必须走国际化资源文件
- 错误码配置对应的国际化消息
- 新增功能必须同步检查国际化的影响范围

---

## code-sec 项目架构规则

### 模块依赖方向（严格单向）

```
common ← es-integration
common ← api ← worker
common ← engine-adapter ← api
common ← engine-adapter ← worker
common ← engine-adapter ← gitlab-integration
```

**规则**：
1. `common` 是基础模块，可被任何模块依赖，但不可依赖其他内部模块
2. `api` 不可依赖 `worker`、`gitlab-integration`、`es-integration`
3. `worker` 可依赖 `api`（暂允许，M2 需解耦为事件驱动），但不可依赖 `gitlab-integration`、`es-integration`
4. `gitlab-integration` 和 `es-integration` 只能依赖 `common`、`engine-adapter` 和 `domain`
5. `engine-adapter` 可依赖 `common` 和 `engine`（适配器模式，需包装 engine）

### api 模块包命名规则

```
com.codesec.api/
├── domain/               # 领域层 — 纯业务概念，零框架依赖
│   ├── entity/           # JPA @Entity
│   ├── enums/            # 业务枚举
│   ├── repository/       # Repository 接口（Spring Data JPA）
│   └── mapper/           # 实体转换
├── application/          # 应用层 — 用例编排
│   └── event/            # 应用事件（如同步索引触发）
├── module/               # 业务模块（按业务领域分包）
│   ├── {domain}/
│   │   ├── controller/   # REST 控制器
│   │   ├── service/      # 应用服务
│   │   └── dto/          # 模块内 DTO
│   └── ...
├── interfaces/           # 接口层 — 跨模块共享的 DTO/适配器
│   └── dto/              # 通用 DTO（如 PaginatedResult）
├── infrastructure/       # 基础设施层 — 技术实现
│   └── queue/            # 队列实现（如 InMemoryScanQueue）
├── security/             # 安全横切关注点
└── config/               # Spring 配置、全局异常处理
```

### 包依赖方向（稳定依赖原则）

```
security/  config/  (横切，可依赖任何层)
     ↕          ↕
infrastructure/ → application/ → domain/
     ↕                          ↕
     interface/dto (可被任意层使用)
     ↕
module/*/controller/ → module/*/service/ → domain/
```

**规则**：
1. `domain/` → 零外部依赖，不可依赖 `module/`、`infrastructure/`、`interfaces/`
2. `application/` → 可依赖 `domain/`，不可依赖 `module/`、`infrastructure/`
3. `infrastructure/` → 可依赖 `domain/`，不可依赖 `application/`、`module/`
4. `module/*/controller/` → 可依赖 `module/*/service/`，不可直接依赖 `domain/repository/`
5. `module/*/service/` → 可依赖 `domain/`、`interfaces/dto/`
6. `interfaces/dto/` → 纯 POJO，零内部依赖
7. `security/` → 可依赖 `domain/` 和 `interfaces/dto/`，不可依赖 `module/`

### 禁止模式

1. **Mock 在生产代码中**：临时开发桩（stub）必须放在 `dev` 包下，标注 `@Deprecated(forRemoval = true)`，并附带替换计划注释
2. **双向模块依赖**：禁止两个内部模块互相依赖。若出现，抽取共享接口到 `common`
3. **Controller 在 `controller/` 顶层**：所有 Controller 必须放在 `module/{domain}/controller/` 下
4. **DTO 在 `domain/dto/`**：DTO 是接口层概念，必须放在 `interfaces/dto/` 或 `module/*/dto/`
5. **基础设施代码在 `module/` 下**：队列、缓存、外部客户端等基础设施实现必须放在 `infrastructure/` 下
6. **事件在顶层 `event/`**：应用事件必须放在 `application/event/` 下
