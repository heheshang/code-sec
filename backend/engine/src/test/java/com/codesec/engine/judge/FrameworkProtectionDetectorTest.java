package com.codesec.engine.judge;

import com.codesec.engine.model.Finding;
import com.codesec.engine.parser.ParsedFile;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("FrameworkProtectionDetector")
class FrameworkProtectionDetectorTest {

    // ---- helpers ----

    private static ParsedFile javaFile(String name, String sourceCode) {
        return new ParsedFile(Path.of(name), "java", sourceCode, null);
    }

    private static Finding finding(Path filePath, int line) {
        return Finding.builder()
            .filePath(filePath.toString())
            .lineStart(line)
            .build();
    }

    private static ProtectionRule annotationRule(String shortName, String fqn, String reason) {
        return new ProtectionRule(
            reason,
            ProtectionRule.Type.ANNOTATION,
            new ProtectionRule.MatchSpec(fqn, "method", null, null),
            reason
        );
    }

    private static ProtectionRule classAnnotationRule(String fqn, String reason) {
        return new ProtectionRule(
            reason,
            ProtectionRule.Type.CLASS_ANNOTATION,
            new ProtectionRule.MatchSpec(fqn, "class", null, null),
            reason
        );
    }

    private static ProtectionRule methodCallRule(String methodName, String className, String reason) {
        return new ProtectionRule(
            reason,
            ProtectionRule.Type.METHOD_CALL,
            new ProtectionRule.MatchSpec(null, null, methodName, className),
            reason
        );
    }

    private ProjectCallGraph buildGraph(List<ParsedFile> files) {
        return new CallGraphBuilder().build(files);
    }

    // ---- method-level annotation tests ----

    @Test
    @DisplayName("classify returns No when method has @PreAuthorize annotation")
    void classify_methodWithPreAuthorize_saysNo() {
        String src = """
            package com.example;
            import org.springframework.security.access.prepost.PreAuthorize;
            public class UserController {
                @PreAuthorize("hasRole('ADMIN')")
                public void adminEndpoint() {
                    userDao.findById(1);
                }
            }
            """;

        ProjectCallGraph graph = buildGraph(List.of(javaFile("UserController.java", src)));
        ProtectionRule rule = annotationRule(
            "PreAuthorize",
            "org.springframework.security.access.prepost.PreAuthorize",
            "Spring Security @PreAuthorize 注解保护"
        );
        FrameworkProtectionDetector detector = new FrameworkProtectionDetector(graph, List.of(rule));

        AlgorithmResult result = detector.classify(
            finding(Path.of("UserController.java"), 5));

        assertInstanceOf(AlgorithmResult.No.class, result);
        assertEquals("Spring Security @PreAuthorize 注解保护", ((AlgorithmResult.No) result).reason());
    }

    @Test
    @DisplayName("classify returns No when method has @Secured annotation")
    void classify_methodWithSecured_saysNo() {
        String src = """
            package com.example;
            import org.springframework.security.access.annotation.Secured;
            public class AdminService {
                @Secured("ROLE_ADMIN")
                public void adminMethod() {}
            }
            """;

        ProjectCallGraph graph = buildGraph(List.of(javaFile("AdminService.java", src)));
        ProtectionRule rule = annotationRule(
            "Secured",
            "org.springframework.security.access.annotation.Secured",
            "Spring Security @Secured 注解保护"
        );
        FrameworkProtectionDetector detector = new FrameworkProtectionDetector(graph, List.of(rule));

        AlgorithmResult result = detector.classify(
            finding(Path.of("AdminService.java"), 5));

        assertInstanceOf(AlgorithmResult.No.class, result);
        assertEquals("Spring Security @Secured 注解保护", ((AlgorithmResult.No) result).reason());
    }

    @Test
    @DisplayName("classify returns No when method has @RolesAllowed annotation")
    void classify_methodWithRolesAllowed_saysNo() {
        String src = """
            package com.example;
            import javax.annotation.security.RolesAllowed;
            public class RoleService {
                @RolesAllowed("admin")
                public void restrictedMethod() {}
            }
            """;

        ProjectCallGraph graph = buildGraph(List.of(javaFile("RoleService.java", src)));
        ProtectionRule rule = annotationRule(
            "RolesAllowed",
            "javax.annotation.security.RolesAllowed",
            "JSR-250 @RolesAllowed 注解保护"
        );
        FrameworkProtectionDetector detector = new FrameworkProtectionDetector(graph, List.of(rule));

        AlgorithmResult result = detector.classify(
            finding(Path.of("RoleService.java"), 5));

        assertInstanceOf(AlgorithmResult.No.class, result);
    }

    // ---- class-level annotation tests ----

    @Test
    @DisplayName("classify returns No when controller class has @PreAuthorize")
    void classify_controllerClassWithPreAuthorize_saysNo() {
        String src = """
            package com.example;
            import org.springframework.security.access.prepost.PreAuthorize;
            @PreAuthorize("hasRole('ADMIN')")
            public class SecureController {
                public void endpoint() {
                    userDao.findById(1);
                }
            }
            """;

        ProjectCallGraph graph = buildGraph(List.of(javaFile("/tmp/com/example/SecureController.java", src)));
        ProtectionRule rule = classAnnotationRule(
            "org.springframework.security.access.prepost.PreAuthorize",
            "Spring Security @PreAuthorize 类级别保护"
        );
        Map<String, String> sourceFiles = Map.of(
            "/tmp/com/example/SecureController.java", src
        );
        FrameworkProtectionDetector detector =
            new FrameworkProtectionDetector(graph, List.of(rule), sourceFiles);

        AlgorithmResult result = detector.classify(
            finding(Path.of("/tmp/com/example/SecureController.java"), 7));

        assertInstanceOf(AlgorithmResult.No.class, result);
        assertEquals("Spring Security @PreAuthorize 类级别保护", ((AlgorithmResult.No) result).reason());
    }

    // ---- method-call tests ----

    @Test
    @DisplayName("classify returns No when vulnerable method calls MyBatis selectOne")
    void classify_methodCallsSqlSessionSelectOne_saysNo() {
        String dao = """
            package com.example;
            import org.apache.ibatis.session.SqlSession;
            public class UserDao {
                public User findById(SqlSession sqlSession, String id) {
                    return sqlSession.selectOne("users.findById", id);
                }
            }
            """;

        ProjectCallGraph graph = buildGraph(List.of(javaFile("/tmp/com/example/UserDao.java", dao)));
        ProtectionRule rule = methodCallRule(
            "selectOne",
            "org.apache.ibatis.session.SqlSession",
            "MyBatis #{} 参数绑定（自动 PreparedStatement 转义）"
        );
        FrameworkProtectionDetector detector = new FrameworkProtectionDetector(graph, List.of(rule));

        AlgorithmResult result = detector.classify(
            finding(Path.of("/tmp/com/example/UserDao.java"), 6));

        assertInstanceOf(AlgorithmResult.No.class, result);
        assertEquals("MyBatis #{} 参数绑定（自动 PreparedStatement 转义）",
            ((AlgorithmResult.No) result).reason());
    }

    @Test
    @DisplayName("classify returns No when vulnerable method calls ESAPI encodeForSQL")
    void classify_methodCallsESAPIEncodeForSQL_saysNo() {
        String src = """
            package com.example;
            import org.owasp.esapi.Encoder;
            public class QueryBuilder {
                public String safeQuery(Encoder encoder, String input) {
                    return encoder.encodeForSQL(codec, input);
                }
            }
            """;

        ProjectCallGraph graph = buildGraph(List.of(javaFile("/tmp/com/example/QueryBuilder.java", src)));
        ProtectionRule rule = methodCallRule(
            "encodeForSQL",
            "org.owasp.esapi.Encoder",
            "ESAPI encoder.encodeForSQL() 保护"
        );
        FrameworkProtectionDetector detector = new FrameworkProtectionDetector(graph, List.of(rule));

        AlgorithmResult result = detector.classify(
            finding(Path.of("/tmp/com/example/QueryBuilder.java"), 6));

        assertInstanceOf(AlgorithmResult.No.class, result);
        assertEquals("ESAPI encoder.encodeForSQL() 保护", ((AlgorithmResult.No) result).reason());
    }

    @Test
    @DisplayName("classify returns Undetermined when no rule matches the unprotected method")
    void classify_unprotectedMethod_saysUndetermined() {
        String src = """
            package com.example;
            public class PlainService {
                public void doWork() {
                    System.out.println("work");
                }
            }
            """;

        ProjectCallGraph graph = buildGraph(List.of(javaFile("/tmp/com/example/PlainService.java", src)));
        ProtectionRule rule = annotationRule(
            "PreAuthorize",
            "org.springframework.security.access.prepost.PreAuthorize",
            "Spring Security @PreAuthorize 注解保护"
        );
        FrameworkProtectionDetector detector = new FrameworkProtectionDetector(graph, List.of(rule));

        AlgorithmResult result = detector.classify(
            finding(Path.of("/tmp/com/example/PlainService.java"), 4));

        assertInstanceOf(AlgorithmResult.Undetermined.class, result);
    }

    @Test
    @DisplayName("classify returns Undetermined when vulnerable method is not in the call graph")
    void classify_methodNotFound_saysUndetermined() {
        ProjectCallGraph graph = new ProjectCallGraph();
        FrameworkProtectionDetector detector = new FrameworkProtectionDetector(graph);

        AlgorithmResult result = detector.classify(
            finding(Path.of("/tmp/com/example/Unknown.java"), 10));

        assertInstanceOf(AlgorithmResult.Undetermined.class, result);
    }

    // ---- YAML loading tests ----

    @Test
    @DisplayName("loadDefaultRules loads all four YAML files with ~10 rules")
    void loadDefaultRules_loadsAllFourFiles() {
        ProjectCallGraph graph = new ProjectCallGraph();
        FrameworkProtectionDetector detector = new FrameworkProtectionDetector(graph);

        int count = detector.ruleCount();
        assertTrue(count >= 8, "Expected at least 8 rules across 4 YAML files, got " + count);
    }

    @Test
    @DisplayName("loadDefaultRules throws descriptive error for malformed YAML")
    void loadDefaultRules_handlesMalformedYAML_throwsDescriptiveError() {
        // Missing required 'name' field
        Map<String, ?> malformed = Map.of(
            "type", "annotation",
            "reason", "missing name"
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> FrameworkProtectionDetector.parseRule(malformed, "test.yml"));
        assertTrue(ex.getMessage().contains("missing"),
            "Error should describe the issue, got: " + ex.getMessage());
    }

    @Test
    @DisplayName("parseRule throws for unknown type values")
    void parseRule_unknownType_throwsError() {
        Map<String, ?> rule = Map.of(
            "name", "Bad Rule",
            "type", "invalid-type",
            "reason", "Should fail",
            "match", Map.of("annotation", "com.example.Ann")
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> FrameworkProtectionDetector.parseRule(rule, "test.yml"));
        assertTrue(ex.getMessage().contains("unknown type"),
            "Error should mention unknown type, got: " + ex.getMessage());
    }

    // ---- callChainProtected tests ----

    @Test
    @DisplayName("callChainProtected finds protected ancestor in reachable chain")
    void callChainProtected_findsProtectedAncestor() {
        String controllerSrc = """
            package com.example;
            public class MyController {
                @GetMapping("/users")
                public void handle() {
                    new MyService().process();
                }
            }
            """;
        String serviceSrc = """
            package com.example;
            import org.owasp.esapi.Encoder;
            public class MyService {
                public void process() {
                    encoder.encodeForSQL(codec, input);
                }
            }
            """;

        ProjectCallGraph graph = buildGraph(List.of(
            javaFile("/tmp/com/example/MyController.java", controllerSrc),
            javaFile("/tmp/com/example/MyService.java", serviceSrc)
        ));

        FrameworkProtectionDetector detector = new FrameworkProtectionDetector(graph, List.of());
        MethodNode serviceMethod = graph.getMethodByKey(
            "com.example.MyService.process()").orElseThrow();

        boolean protected_ = detector.callChainProtected(serviceMethod, "encodeForSQL");
        assertTrue(protected_,
            "The call chain should detect encodeForSQL in the reachable service method");
    }

    @Test
    @DisplayName("callChainProtected returns false when no entry points exist")
    void callChainProtected_noEntryPoints_returnsFalse() {
        ProjectCallGraph graph = new ProjectCallGraph();
        MethodNode plain = MethodNode.of("com.example.Util", "helper", List.of(), "void", List.of());
        graph.addMethod(plain);

        FrameworkProtectionDetector detector = new FrameworkProtectionDetector(graph, List.of());
        assertFalse(detector.callChainProtected(plain, "selectOne"));
    }

    // ---- extractClassAnnotations tests ----

    @Test
    @DisplayName("extractClassAnnotations returns class-level annotations for a method")
    void extractClassAnnotations_returnsClassLevelAnnotations() {
        String src = """
            package com.example;
            @Deprecated
            @SuppressWarnings("unchecked")
            public class MyController {
                public void handle() {
                    System.out.println("hello");
                }
            }
            """;

        FrameworkProtectionDetector detector = new FrameworkProtectionDetector(new ProjectCallGraph());
        MethodNode method = MethodNode.of(
            "com.example.MyController", "handle", List.of(), "void", List.of(), 7, 9, false, true);

        List<String> annotations = detector.extractClassAnnotations(src, method);

        assertTrue(annotations.contains("Deprecated"),
            "Should detect @Deprecated on class, got: " + annotations);
        assertTrue(annotations.contains("SuppressWarnings"),
            "Should detect @SuppressWarnings on class, got: " + annotations);
    }

    @Test
    @DisplayName("extractClassAnnotations for method in nested class returns outer class annotations")
    void extractClassAnnotations_methodInNestedClass_returnsOuterClassAnnotations() {
        String src = """
            package com.example;
            @PreAuthorize("hasRole('ADMIN')")
            public class Outer {
                public class Inner {
                    public void nestedMethod() {}
                }
            }
            """;

        FrameworkProtectionDetector detector = new FrameworkProtectionDetector(new ProjectCallGraph());
        MethodNode method = MethodNode.of(
            "com.example.Outer.Inner", "nestedMethod", List.of(), "void", List.of(), 5, 5, false, true);

        List<String> annotations = detector.extractClassAnnotations(src, method);

        assertTrue(annotations.contains("PreAuthorize"),
            "Should detect @PreAuthorize from outer class, got: " + annotations);
    }

    @Test
    @DisplayName("extractClassAnnotations returns empty list when source can't be parsed")
    void extractClassAnnotations_brokenSource_returnsEmptyList() {
        FrameworkProtectionDetector detector = new FrameworkProtectionDetector(new ProjectCallGraph());
        MethodNode method = MethodNode.of(
            "com.example.Broken", "method", List.of(), "void", List.of(), 3, 3, false, true);

        List<String> annotations = detector.extractClassAnnotations("invalid java {{", method);
        assertTrue(annotations.isEmpty(), "Should return empty list for unparseable source");
    }

    // ---- multiple rules priority ----

    @Test
    @DisplayName("classify matches the first applicable rule when multiple rules exist")
    void classify_multipleRules_matchesFirstApplicable() {
        String src = """
            package com.example;
            import org.springframework.security.access.prepost.PreAuthorize;
            public class MultiController {
                @PreAuthorize("hasRole('ADMIN')")
                public void endpoint() {}
            }
            """;

        ProjectCallGraph graph = buildGraph(List.of(javaFile("MultiController.java", src)));
        ProtectionRule rule1 = annotationRule(
            "Secured", "org.springframework.security.access.annotation.Secured", "lower priority");
        ProtectionRule rule2 = annotationRule(
            "PreAuthorize", "org.springframework.security.access.prepost.PreAuthorize", "higher priority");

        FrameworkProtectionDetector detector = new FrameworkProtectionDetector(
            graph, List.of(rule1, rule2));

        AlgorithmResult result = detector.classify(
            finding(Path.of("MultiController.java"), 5));

        assertInstanceOf(AlgorithmResult.No.class, result);
        assertEquals("higher priority", ((AlgorithmResult.No) result).reason(),
            "Should return the first matching rule's reason (rule2 is matched but appears after rule1 which doesn't match)");
    }
}
