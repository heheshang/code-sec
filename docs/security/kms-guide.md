# KMS (Key Management Service) 运维指南

> 阿里云 KMS 集成——密钥管理与加密服务。
> 本文档面向运维人员，涵盖控制台操作、配置管理、故障处理等。

---

## 目录

- [架构概览](#架构概览)
- [前置条件](#前置条件)
- [阿里云控制台操作](#阿里云控制台操作)
- [本地配置](#本地配置)
- [使用方式](#使用方式)
- [密钥轮转](#密钥轮转)
- [存量数据迁移](#存量数据迁移)
- [监控与告警](#监控与告警)
- [故障处理](#故障处理)
- [安全注意事项](#安全注意事项)

---

## 架构概览

```
┌──────────────┐     ┌─────────────────────┐     ┌──────────────────┐
│  Application │────>│  CryptoService      │────>│  AES (本地模式)   │
│  (Spring)    │     │  (接口层)            │     │  KMS (阿里云)     │
│              │     │                     │     │                  │
│  provider:   │     │  KmsCryptoService   │────>│  aliyun-java-sdk  │
│  aes | kms   │     │                     │     │  → kms.aliyuncs   │
└──────────────┘     └─────────────────────┘     └──────────────────┘
```

### 关键组件

| 组件 | 职责 | 文件 |
|------|------|------|
| `CryptoService` | 加密/解密接口 + rotate/isKms | `crypto/CryptoService.java` |
| `AesGcmCryptoService` | AES-256-GCM 本地实现 | `crypto/AesGcmCryptoService.java` |
| `KmsCryptoService` | 阿里云 KMS 实现，含 AES 回退 | `crypto/KmsCryptoService.java` |
| `CryptoConfig` | provider 选择：aes / kms | `crypto/CryptoConfig.java` |
| `KeyRotationService` | 密钥轮转编排 | `crypto/KeyRotationService.java` |
| `KmsConfig` | KMS 配置属性绑定 | `crypto/KmsConfig.java` |

---

## 前置条件

1. **阿里云账号** — 已开通 KMS 服务
2. **AccessKey** — 具备 KMS 权限的 RAM 用户 AccessKey
3. **KMS Key** — 已创建对称加密的 CMK（Customer Master Key）
4. **网络连通** — 应用服务器可访问 `kms.{region}.aliyuncs.com`

### 权限要求

RAM 策略至少包含：

```json
{
  "Version": "1",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "kms:Encrypt",
        "kms:Decrypt",
        "kms:DescribeKey",
        "kms:CreateKeyVersion",
        "kms:ListKeyVersions"
      ],
      "Resource": "*"
    }
  ]
}
```

---

## 阿里云控制台操作

### 1. 创建 CMK

1. 登录 [阿里云 KMS 控制台](https://kms.console.aliyun.com/)
2. 选择地域（建议与应用同地域，如 `cn-hangzhou`）
3. 点击 **创建密钥**
4. 选择 **对称加密** → **Aliyun_AES_256**
5. 填写别名（如 `codesec-cmk`）和描述
6. 记录生成的 **密钥 ID**（格式：`xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`）

### 2. 密钥轮转

1. 在 KMS 控制台打开 **自动轮转**（可选，按合规要求配置周期）
2. 手动轮转：调用 `POST /api/v1/admin/crypto/rotate` 接口（见下文）
3. 手动轮转会在 KMS 创建新密钥版本，旧版本保留用于解密存量数据

### 3. 查看操作审计

- KMS 控制台 → **操作审计** 查看所有加解密调用记录
- 或通过 ActionTrail 导出到 SLS/OSS

---

## 本地配置

### application.yml

```yaml
codesec:
  crypto:
    provider: kms                # aes | kms
    master-key: ""               # AES 模式需要；KMS 模式留空
    kms:
      region: cn-hangzhou        # 阿里云地域
      key-id: your-cmk-key-id    # KMS 密钥 ID
```

### 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `KMS_REGION` | KMS 地域 | `cn-hangzhou` |
| `KMS_KEY_ID` | KMS 密钥 ID | 无 |
| `ALIBABA_CLOUD_ACCESS_KEY_ID` | RAM 用户 AccessKeyId | 无（使用默认凭据链） |
| `ALIBABA_CLOUD_ACCESS_KEY_SECRET` | RAM 用户 AccessKeySecret | 无（使用默认凭据链） |

> **凭据链说明**：KMS SDK 会自动按顺序读取：
> 1. 环境变量 `ALIBABA_CLOUD_ACCESS_KEY_ID` / `ALIBABA_CLOUD_ACCESS_KEY_SECRET`
> 2. Java 系统属性 `alibabacloud.accessKeyId` / `alibabacloud.accessKeySecret`
> 3. 默认凭据文件 `~/.alibabacloud/credentials`
> 4. ECS 实例 RAM Role（适用于阿里云 ECS 部署）

### 切换模式

**从 AES 切换到 KMS:**

```bash
# 1. 部署 KMS 配置的版本（provider: kms）
# 2. 运行存量迁移（dry-run 预览）
./scripts/migrate-kms.sh --dry-run

# 3. 确认无误后执行迁移
./scripts/migrate-kms.sh

# 4. 验证新数据使用 KMS 加密
curl -X POST /api/v1/admin/crypto/rotate
# 返回: {"status":"ok","provider":"kms","rotatedAt":"..."}
```

---

## 使用方式

### 运行时选择

应用通过 `codesec.crypto.provider` 配置选择加密实现：

| provider | 实现 | 说明 |
|----------|------|------|
| `aes` | `AesGcmCryptoService` | 本地 AES-256-GCM，性能高，无需网络 |
| `kms` | `KmsCryptoService` | 阿里云 KMS，密钥由云平台管理，合规性强 |

### 代码调用

所有加密操作通过 `CryptoService` 接口进行，业务层无需关心具体实现：

```java
public class RepoService {
    private final CryptoService cryptoService;

    public RepoResponse create(RepoCreateRequest req) {
        // 自动使用当前 provider 加密
        String encrypted = cryptoService.encrypt(req.getAccessToken());
        // ...
    }
}
```

---

## 密钥轮转

### 触发轮转

```bash
curl -X POST http://localhost:8080/api/v1/admin/crypto/rotate \
  -H "Authorization: Bearer <admin-token>"
```

### 响应示例

```json
{
  "status": "ok",
  "provider": "kms",
  "rotatedAt": "2026-07-01T10:30:00Z"
}
```

### 轮转行为

| provider | 行为 |
|----------|------|
| `aes` | 无操作（密钥由配置文件固定） |
| `kms` | 调用 KMS CreateKeyVersion 创建新密钥版本 |

> **注意**：轮转仅创建新密钥版本，存量加密数据仍可用旧版本解密。
> KMS 的自动解密会使用对应版本进行解密。

---

## 存量数据迁移

当从 AES 切换到 KMS 时，已加密的存量数据需要迁移。

### 迁移原理

```
存量 AES 密文 → AesGcmCryptoService.decrypt() → 明文
               → KmsCryptoService.encrypt()    → KMS 密文
```

### 执行迁移

```bash
# 1. 构建 API jar
cd backend && mvn -pl common,api package -DskipTests

# 2. 预览迁移影响
./scripts/migrate-kms.sh --dry-run

# 3. 执行迁移（需要应用正在运行）
./scripts/migrate-kms.sh
```

`KmsMigrationRunner` 会在 `kms-migrate` profile 下启动，自动完成解密→加密→更新。

---

## 监控与告警

### 关键指标

| 指标 | 说明 | 建议阈值 |
|------|------|----------|
| KMS 请求延迟 | Encrypt/Decrypt API 耗时 | > 1s 告警 |
| KMS 回退次数 | KMS 不可用时 AES 回退计数 | > 0 告警 |
| 轮转失败次数 | rotate 接口返回 failed | > 0 告警 |

### KMS 可用性

- `KmsCryptoService` 启动时自动检测 KMS 连通性
- 启动后 KMS 不可用则打印 WARN 日志并回退 AES
- 运行时 KMS 不可用则每请求回退 AES（打印 ERROR 日志）

### 日志关键词

| 日志 | 级别 | 含义 |
|------|------|------|
| `KMS client initialized` | INFO | KMS 连接正常 |
| `KMS unavailable, AES fallback` | WARN | KMS 不可用，回退 AES |
| `KMS encrypt/decrypt failed, AES fallback` | ERROR | 运行时 KMS 调用失败 |
| `Key rotation succeeded` | INFO | 轮转成功 |
| `Key rotation failed` | ERROR | 轮转失败 |

---

## 故障处理

### KMS 连接失败

**现象**: 日志出现 `KMS unavailable, AES fallback`

**排查步骤**:

1. 检查网络连通性
   ```bash
   curl -v https://kms.cn-hangzhou.aliyuncs.com
   ```

2. 检查 AccessKey 是否有效
   ```bash
   # 通过阿里云 CLI 验证
   aliyun kms DescribeKey --KeyId <your-key-id>
   ```

3. 检查 RAM 权限策略是否包含 `kms:DescribeKey`

4. 检查 KMS 密钥状态是否为 **已启用**

### 加解密失败

**现象**: `KMS encrypt failed` 或 `KMS decrypt failed`

**排查步骤**:

1. 确认密钥 ID 正确
2. 确认密钥未禁用或计划删除
3. 确认密文数据未被篡改（AES 回退时不会影响 KMS 密文）
4. 查看 KMS 控制台操作审计

### 轮转失败

**现象**: `Key rotation failed`

**原因**:
- 密钥类型不支持自动轮转（仅对称密钥支持）
- RAM 权限缺少 `kms:CreateKeyVersion`
- KMS 服务不可用

### 迁移失败

**现象**: 迁移脚本报错

**常见原因**:
- jar 未构建
- 数据库连接信息不对
- 迁移过程中 provider 切换导致 re-encrypt 使用错误 provider

---

## 安全注意事项

1. **生产环境必须使用 KMS 模式**：AES 本地模式仅用于开发/测试
2. **AccessKey 管理**：不要硬编码在配置文件或代码中，使用 RAM Role（ECS）或凭据管家
3. **KMS 密钥删除**：CMK 删除有 7-30 天等待期，确保密钥不再使用
4. **密钥轮转**：建议配置自动轮转（90 天/180 天），或定期手动轮转
5. **网络隔离**：KMS API 建议通过私网访问（VPC Endpoint），避免公网传输
6. **审计日志**：开启 ActionTrail 记录所有 KMS 调用

### 私网访问配置

```yaml
codesec:
  crypto:
    kms:
      region: cn-hangzhou
      # 使用私网域名（需先创建 VPC Endpoint）
      # 访问控制台：VPC → 终端节点 → 创建终端节点 → 选择 kms.{region}.aliyuncs.com
```

---

## 参考链接

- [阿里云 KMS 文档](https://help.aliyun.com/product/28933.html)
- [RAM 权限策略](https://ram.console.aliyun.com/policies)
- [VPC Endpoint 配置](https://help.aliyun.com/document_detail/120825.html)
- [Java SDK for KMS](https://help.aliyun.com/document_detail/69035.html)
