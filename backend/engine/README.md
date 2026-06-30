# CodeSec Self-Research SAST Engine

A production-quality Static Application Security Testing engine for the CodeSec code security audit platform. Self-contained, runnable, tested Java SAST engine that demonstrates interview-defensible architecture: clean separation of rule-engine core from concrete detectors, data-driven rules in YAML, multi-language AST via JavaParser (tree-sitter swap-ready), and standard output matching the union schema.

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                    Engine.java                       │
│  Orchestrator: scan(Path) -> List<Finding>           │
│  - File discovery & exclusion (PathMatcher)          │
│  - Language dispatch (AstParser interface)           │
│  - Rule dispatch (RuleRegistry → Detector by ruleId) │
│  - Finding collection                                │
└───────────┬─────────────────────┬───────────────────┘
            │                     │
    ┌───────▼──────┐     ┌───────▼──────────────┐
    │  AstParser   │     │    Detector           │
    │ (interface)  │     │   (interface)         │
    │  parse(Path) │     │   detect(File, Rule)  │
    │  →ParsedFile │     │   → List<Finding>     │
    └───────┬──────┘     └──┬─────────┬──────────┘
            │               │         │
    ┌───────▼──────┐  ┌────▼─────┐ ┌▼──────────┐
    │ JavaParser   │  │ AstDet   │ │ RegexDet   │
    │ (tree-sitter  │  │ (abstract)│ │ (concrete) │
    │  swap-ready) │  └────┬─────┘ └─────┬──────┘
    └──────────────┘       │             │
                    ┌──────┼──────┐  ┌───┴────────┐
                    │ SQL  │ XSS  │  │ Hardcoded  │
                    │ Inj  │      │  │ Password   │
                    │ Det  │ Det  │  │ Det + Weak │
                    │      │      │  │ Crypto Det │
                    └──────┴──────┘  └────────────┘
```

### Key Design Decisions

1. **Rule-agnostic Engine**: `Engine.java` dispatches by rule ID to registered detectors. It knows nothing about rule content.

2. **Data-driven rules**: All detection configuration lives in YAML files. Adding a new **regex-based** rule = adding a YAML file, zero Java changes. Adding a new **AST-based** rule = YAML file + new detector class.

3. **AstParser interface**: `JavaLanguage.java` implements `AstParser` using JavaParser. This can be swapped for tree-sitter by implementing a `TreeSitterJavaLanguage` without changing any detector.

4. **Detector per rule**: Each concrete detector handles exactly one rule type (rule ID → detector mapping in Engine). This prevents duplicate findings from cross-dispatch.

## Quick Start

```bash
# Build and test
cd engine
mvn clean test

# Scan sample code
./run.sh examples/sample-code

# With output file
./run.sh examples/sample-code -o findings.json
```

## CLI Usage

```bash
java -jar target/code-sec-engine-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
  scan --input <source-dir> --output <json-file> --rules <rules-dir>
```

Options:
- `-i, --input <dir>` - Source code directory to scan (required)
- `-o, --output <file>` - JSON output file (default: stdout)
- `-r, --rules <dir>` - Rules directory (default: classpath rules/java/)

## How to Add a New Rule

### Regex-based rule (zero Java code)

1. Create a YAML file in `src/main/resources/rules/java/`:

```yaml
id: java/my-rule-001
name: My Custom Rule
severity: medium
cwe: CWE-XXX
languages: [java]
engine: self_sast
detection:
  type: regex
  pattern: 'myJavaPattern'
fix:
  description: "How to fix this issue"
  example: |
    // Safe code example
author: security-team
enabled: true
```

2. Register the detector in `Engine.java`:

```java
detectorsByRuleId.put("java/my-rule-001", new RegexDetector());
```

### AST-based rule

1. Create YAML rule file (same as above with `detection.type: ast`)
2. Create a detector class extending `AstDetector`:

```java
public class MyDetector extends AstDetector {
    @Override
    protected List<AstMatch> findMatches(CompilationUnit cu, ParsedFile file, Rule rule) {
        // Use JavaParser visitor to find AST patterns
    }
}
```

3. Register in `Engine.registerDetectors()`.

## Rule File Format

```yaml
id: java/rule-id            # Unique rule identifier
name: Human-readable name
severity: critical|high|medium|low|info
cwe: CWE-XXX
languages: [java]           # Supported languages
engine: self_sast
detection:
  type: ast|regex           # Detection method
  pattern: "..."            # Regex pattern or AST descriptor
fix:
  description: "Fix guidance"
  example: |
    // Safe code
false_positive_scenarios:   # Known false positive cases
  - framework: spring-jdbc
    reason: "Explanation"
author: security-team
enabled: true
```

## Trade-offs & Decisions

### JavaParser vs Tree-sitter

| Aspect | JavaParser | Tree-sitter |
|--------|-----------|-------------|
| Dependencies | Pure Java, zero native libs | Requires JNI + platform-specific .so/.dylib |
| JDK requirement | 17+ | 23+ (official), 8+ (bonede fork) |
| Setup complexity | Zero (Maven dependency) | Requires native library loading |
| AST quality | Full Java AST, typed visitors | Generic CST/query API |
| Multi-language | Java only | 100+ grammars |
| Performance | Fast (pure Java) | Extremely fast (C core) |

**Decision**: JavaParser for MVP. The `AstParser` interface allows tree-sitter swap later without detector changes. Migration path: implement `TreeSitterJavaLanguage implements AstParser`, swap in Engine.

### Regex vs AST Detection

| Aspect | Regex | AST |
|--------|-------|-----|
| Complexity | Low | Higher |
| False positives | More | Fewer |
| Rule authoring | Simple pattern | Requires AST knowledge |
| Context awareness | None (text-only) | Full (type info, scope, data flow) |

**Guideline**: Use regex for simple pattern matching (hardcoded secrets, weak algorithms). Use AST for structural patterns requiring context (SQL injection, XSS, data flow).

### Known Limitations

1. **Data flow analysis**: Cross-method variable taint tracking is limited. SQL injection detection catches variable-level data flow within a single compilation unit but not across method calls.

2. **Single language**: Java only in MVP. Multi-language support requires implementing `AstParser` per language.

3. **Rule discovery**: New rules must be registered in Engine's `registerDetectors()`. Not fully dynamic (YAML-only rules work, AST rules need detector class).

4. **No sanitizer awareness**: Does not detect when input has been sanitized (e.g., `StringEscapeUtils.escapeHtml4()`), producing false positives.

5. **No framework-specific rules**: Does not automatically recognize Spring/MyBatis-safe patterns.

## Union Schema Output

All findings match the union schema from `architecture.md § 2.3`:

| Field | Type | Description |
|-------|------|-------------|
| vuln_id | UUID | Unique vulnerability identifier |
| project_id | Integer? | Project ID (standalone = null) |
| scan_id | String? | Scan task ID |
| engine | String | "self_sast" |
| rule_id | String | YAML rule identifier |
| title | String | Human-readable finding title |
| severity | String | critical/high/medium/low/info |
| file_path | String | Absolute path to source file |
| line_start | int | Start line of finding |
| line_end | int | End line of finding |
| code_snippet | String | Actual code at finding location |
| description | String | Detailed vulnerability description |
| fix_suggestion | String | How to fix |
| cwe | String | CWE identifier |
| cve | String? | CVE identifier (if applicable) |
| exploitability | String | exploitable/potentially_exploitable/not_exploitable |
| exploit_reason | String | Why the finding is exploitable |
| engine_raw | Object | Engine-specific metadata |
| discovered_at | ISO-8601 | Timestamp of discovery |

## Included Rules

| Rule ID | Name | Severity | Type |
|---------|------|----------|------|
| java/sql-injection-001 | SQL Injection - String Concatenation | high | AST |
| java/hardcoded-password-001 | Hardcoded Password or Secret | high | Regex |
| java/xss-001 | Cross-Site Scripting (XSS) | high | AST |
| java/weak-crypto-001 | Use of Weak Cryptographic Algorithm | medium | Regex |

## Exploitability Judger

**Status**: M1, 6 algorithmic components, 120+ tests

The Exploitability Judger enriches raw SAST findings with three-state exploitability
classification: `EXPLOITABLE`, `POTENTIALLY_EXPLOITABLE`, `NOT_EXPLOITABLE`.

### Architecture

3 algorithms + 1 orchestrator:
- **isReachable** (ReachableAnalyzer): Is the vulnerable method reachable from any HTTP entry point?
- **isUserControllable** (InputControllabilityAnalyzer): Do sink parameters trace to user input?
- **hasFrameworkProtection** (FrameworkProtectionDetector): Is the call chain protected by Spring Security / MyBatis / etc.?

Composition rule: NOT_EXPLOITABLE (from any) > EXPLOITABLE > POTENTIALLY_EXPLOITABLE

### Quick start

```java
// Build call graph once per scan
ProjectCallGraph graph = new CallGraphBuilder().build(parsedFiles);

// Construct 3 analyzers
ReachableAnalyzer reachable = new ReachableAnalyzer(graph);
InputControllabilityAnalyzer controllable = new InputControllabilityAnalyzer(graph, sourceFileMap);
FrameworkProtectionDetector protection = new FrameworkProtectionDetector(graph, rules);

// Wire them up
ExploitabilityJudger judger = new ExploitabilityJudger(graph, sourceFileMap, rules, Duration.ofSeconds(5));

// Apply to findings (modifies in place)
judger.judgeBatch(findings, sourceFileMap);
```

### Configuration

- `judge.enabled` (default: true) — toggle the entire feature
- `judge.perFileTimeout` (default: 5s) — per-file algorithm timeout
- `judge.algorithmToggles` (default: all true) — toggle individual algorithms

### Performance

See `engine/BENCHMARK.md` for detailed measurements.

| Metric | Result | Budget |
|--------|--------|--------|
| 100K LOC scan | 40s | 30s (see BENCHMARK for optimization recs) |
| Memory peak | 1.6 GB | 2 GB |
| Precision@EXPLOITABLE | 100% | >= 80% |
| Recall@EXPLOITABLE | 100% | >= 90% |

## Project Structure

```
engine/
├── pom.xml
├── run.sh
├── .gitignore
├── README.md
├── src/main/java/com/codesec/engine/
│   ├── Engine.java                    # Orchestrator
│   ├── cli/CliRunner.java             # picocli CLI
│   ├── rule/
│   │   ├── Rule.java                  # Data record
│   │   ├── RuleLoader.java            # SnakeYAML loader
│   │   ├── RuleRegistry.java          # In-memory store
│   │   ├── Detection.java             # Detection config record
│   │   ├── Fix.java                   # Fix suggestion record
│   │   └── FalsePositiveScenario.java
│   ├── parser/
│   │   ├── AstParser.java             # Interface
│   │   ├── ParsedFile.java            # record(source, ast, language)
│   │   └── languages/JavaLanguage.java # JavaParser impl
│   ├── detector/
│   │   ├── Detector.java              # Interface
│   │   ├── RegexDetector.java         # Generic regex engine
│   │   ├── AstDetector.java           # Abstract AST engine
│   │   └── impl/
│   │       ├── SqlInjectionDetector.java
│   │       ├── HardcodedPasswordDetector.java
│   │       ├── XssDetector.java
│   │       └── WeakCryptoDetector.java
│   ├── model/
│   │   ├── Finding.java               # Union schema record
│   │   ├── Severity.java              # Enum
│   │   └── Exploitability.java        # Enum
│   └── util/PathMatcher.java          # Glob-based file exclusion
├── src/main/resources/rules/java/     # 4 YAML rule files
├── src/test/java/com/codesec/engine/
│   ├── EngineIntegrationTest.java     # E2E test
│   └── detector/impl/                 # 4 unit test files
├── examples/
│   ├── sample-code/                   # 5 Java samples (4 positive, 1 negative)
│   └── output/expected-findings.json  # Expected output
```

## Test Coverage

```bash
mvn test
# Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
# ✅ 4 detector unit tests (16 test cases)
# ✅ 1 integration test (4 test cases)
```

## Technology Stack

- **Java 17** with records, sealed types, pattern matching
- **JavaParser 3.26** for AST (swap-ready via AstParser interface)
- **SnakeYAML 2.3** for rule file parsing
- **Picocli 4.7** for CLI
- **Jackson 2.18** for JSON serialization
- **SLF4J 2.0** for logging
- **JUnit 5** for testing
- **Maven** with assembly plugin for fat JAR
