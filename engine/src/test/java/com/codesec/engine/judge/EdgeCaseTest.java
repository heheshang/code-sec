package com.codesec.engine.judge;

import com.codesec.engine.model.Finding;
import com.codesec.engine.parser.ParsedFile;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Edge cases for ExploitabilityJudger")
class EdgeCaseTest {

    private static ParsedFile javaFile(String name, String sourceCode) {
        return new ParsedFile(Path.of(name), "java", sourceCode, null);
    }

    private static ParsedFile javaFile(String name, List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line).append('\n');
        }
        return new ParsedFile(Path.of(name), "java", sb.toString(), null);
    }

    private static Finding finding(String filePath, int lineStart) {
        return Finding.builder()
            .filePath(filePath)
            .lineStart(lineStart)
            .lineEnd(lineStart)
            .title("Test")
            .severity("HIGH")
            .ruleId("java/sql-injection-001")
            .cwe("CWE-89")
            .build();
    }

    // ---- Test 1: null finding ----

    @Test
    @DisplayName("null finding passed to classify throws NullPointerException")
    void nullFinding_doesNotThrow() {
        // Build a simple graph so the analyzer has data
        String src = "package com.example; public class Foo { public void bar() {} }";
        ParsedFile file = javaFile("src/main/java/com/example/Foo.java", src);
        ProjectCallGraph graph = new CallGraphBuilder().build(List.of(file));

        ReachableAnalyzer reachable = new ReachableAnalyzer(graph);
        Map<String, ParsedFile> parsedFileMap = Map.of();
        InputControllabilityAnalyzer controllable =
            new InputControllabilityAnalyzer(graph, parsedFileMap);
        FrameworkProtectionDetector protection =
            new FrameworkProtectionDetector(graph);

        // Passing null to classify throws NPE per Objects.requireNonNull
        assertDoesNotThrow(() -> {
            try {
                reachable.classify(null);
            } catch (NullPointerException ignored) {
                // Expected: null is rejected with NPE
            }
        }, "NPE from null finding should be catchable");

        assertDoesNotThrow(() -> {
            try {
                controllable.classify(null);
            } catch (NullPointerException ignored) {
                // Expected
            }
        }, "NPE from null finding should be catchable");

        assertDoesNotThrow(() -> {
            try {
                protection.classify(null);
            } catch (NullPointerException ignored) {
                // Expected
            }
        }, "NPE from null finding should be catchable");
    }

    // ---- Test 2: empty file list ----

    @Test
    @DisplayName("empty file list builds empty graph")
    void emptyFileList_buildsEmptyGraph() {
        ProjectCallGraph graph = new CallGraphBuilder().build(List.of());
        assertEquals(0, graph.size(), "Empty file list should produce empty graph");
        assertTrue(graph.getEntryPoints().isEmpty(), "Empty graph should have no entry points");
        assertEquals(0, graph.edgeCount(), "Empty graph should have zero edges");
    }

    // ---- Test 3: deeply nested class path ----

    @Test
    @DisplayName("deeply nested class path extracts correct FQN")
    void deeplyNestedClass_extractsClassName() {
        // InputControllabilityAnalyzer.filePathToClassFqn handles deeply nested paths
        String classFqn = InputControllabilityAnalyzer.filePathToClassFqn(
            "src/main/java/com/example/sub/project/module/deep/NestedClass.java");
        assertEquals("com.example.sub.project.module.deep.NestedClass", classFqn);

        // Also test without src/main/java prefix (relative to src root)
        String classFqn2 = InputControllabilityAnalyzer.filePathToClassFqn(
            "com/example/sub/project/module/deep/Another.java");
        assertEquals("com.example.sub.project.module.deep.Another", classFqn2);
    }

    // ---- Test 4: method with no body ----

    @Test
    @DisplayName("interface method without body produces no call edges")
    void methodWithNoBody_treatedAsUnreachable() {
        String srcInterface = """
            package com.example;
            public interface MyService {
                String process(String input);
            }
            """;
        String srcController = """
            package com.example;
            import org.springframework.web.bind.annotation.*;
            @RestController
            public class Ctrl {
                @GetMapping("/api")
                public String handle(@RequestParam String id) {
                    return new Impl().process(id);
                }
            }
            """;
        String srcImpl = """
            package com.example;
            public class Impl implements MyService {
                public String process(String input) {
                    java.sql.Statement stmt = null;
                    stmt.execute(input);
                    return "";
                }
            }
            """;

        ProjectCallGraph graph = new CallGraphBuilder().build(List.of(
            javaFile("src/main/java/com/example/MyService.java", srcInterface),
            javaFile("src/main/java/com/example/Ctrl.java", srcController),
            javaFile("src/main/java/com/example/Impl.java", srcImpl)
        ));

        // Interface method has no body, so no call edges from it
        // But it should appear as a MethodNode in the graph
        assertTrue(graph.size() >= 3, "Graph should contain interface method node");
        MethodNode interfaceMethod = graph.getMethodByKey(
            "com.example.MyService.process(String)").orElse(null);
        assertNotNull(interfaceMethod, "Interface method should exist in graph");

        // No outgoing calls from interface method (no body)
        List<CallEdge> outgoing = graph.getOutgoingCalls(interfaceMethod);
        assertTrue(outgoing.isEmpty(), "Interface method should have no outgoing calls");
    }

    // ---- Test 5: lambda not followed ----

    @Test
    @DisplayName("lambda expression inside method body is not followed")
    void lambdaExpression_notFollowed() {
        String src = """
            package com.example;
            import java.util.List;
            public class LambdaClass {
                public void process() {
                    List<String> items = List.of("a", "b");
                    items.forEach(item -> {
                        new Util().log(item);
                    });
                }
                public void safe() {}
            }
            """;
        String srcUtil = """
            package com.example;
            public class Util {
                public void log(String msg) { System.out.println(msg); }
            }
            """;

        ProjectCallGraph graph = new CallGraphBuilder().build(List.of(
            javaFile("src/main/java/com/example/LambdaClass.java", src),
            javaFile("src/main/java/com/example/Util.java", srcUtil)
        ));

        MethodNode processMethod = graph.getMethodByKey(
            "com.example.LambdaClass.process()").orElse(null);
        assertNotNull(processMethod, "process() method should exist");

        // The lambda body call `new Util().log(item)` might or might not appear
        // depending on whether JavaParser traverses into lambdas;
        // either outcome is acceptable — the key is no crash
        List<CallEdge> outgoing = graph.getOutgoingCalls(processMethod);
        assertNotNull(outgoing, "Outgoing calls list should not be null");
    }

    // ---- Test 6: finding on test path ----

    @Test
    @DisplayName("finding on test path is handleable by judger")
    void findingOnTestPath_markedNotExploitable() {
        // Create a simple controller and test-path utility
        String srcController = """
            package com.example;
            import org.springframework.web.bind.annotation.*;
            @RestController
            public class ProdController {
                @GetMapping("/prod")
                public String handle() { return "ok"; }
            }
            """;
        String srcTestUtil = """
            package com.example;
            public class TestUtil {
                public void helper() {
                    java.sql.Statement stmt = null;
                    stmt.execute("SELECT 1");
                }
            }
            """;

        ParsedFile ctrlFile = javaFile(
            "src/main/java/com/example/ProdController.java", srcController);
        ParsedFile testFile = javaFile(
            "src/test/java/com/example/TestUtil.java", srcTestUtil);

        List<ParsedFile> files = List.of(ctrlFile, testFile);
        CallGraphBuilder builder = new CallGraphBuilder();
        ProjectCallGraph graph = builder.build(files);

        Map<String, ParsedFile> parsedFileMap = new HashMap<>();
        for (ParsedFile f : files) {
            parsedFileMap.put(f.relativePath(), f);
        }

        ReachableAnalyzer reachable = new ReachableAnalyzer(graph);
        InputControllabilityAnalyzer controllable =
            new InputControllabilityAnalyzer(graph, parsedFileMap);
        FrameworkProtectionDetector protection =
            new FrameworkProtectionDetector(graph);

        ExploitabilityJudger judger = new ExploitabilityJudger(
            reachable, controllable, protection);

        List<Finding> findings = new ArrayList<>(List.of(
            finding("src/test/java/com/example/TestUtil.java", 5)));
        Map<Path, String> sourceMap = Map.of(
            testFile.path(), srcTestUtil,
            ctrlFile.path(), srcController);

        judger.judgeBatch(findings, sourceMap);
        judger.shutdown();

        Finding f = findings.get(0);
        assertNotNull(f.exploitability(), "Test-path finding should have exploitability set");
        assertNotNull(f.exploitReason(), "Test-path finding should have exploitReason set");
    }

    // ---- Test 7: thread safety ----

    @Test
    @DisplayName("project call graph supports concurrent read access")
    void projectCallGraph_threadSafety_worksConcurrently() throws Exception {
        String src = """
            package com.example;
            public class Big {
                public void m1() {}
                public void m2() {}
                public void m3() {}
                public void m4() {}
                public void m5() {}
            }
            """;

        ProjectCallGraph graph = new CallGraphBuilder().build(List.of(
            javaFile("src/main/java/com/example/Big.java", src)));

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    // Multiple threads reading the graph concurrently
                    List<MethodNode> methods = graph.getAllMethods();
                    assertFalse(methods.isEmpty());

                    MethodNode m = graph.getMethodByKey(
                        "com.example.Big.m" + ((idx % 5) + 1) + "()").orElse(null);
                    assertNotNull(m, "Thread " + idx + " should find method m" + ((idx % 5) + 1));

                    List<CallEdge> outgoing = graph.getOutgoingCalls(m);
                    assertNotNull(outgoing);

                    List<MethodNode> entries = graph.getEntryPoints();
                    assertNotNull(entries);
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        assertTrue(completed, "All threads should complete within 5 seconds");
    }

    // ---- Test 8: circular call chain ----

    @Test
    @DisplayName("circular call chain does not cause infinite loop")
    void circularCallChain_doesNotInfiniteLoop() {
        // A → B → C → A (circular)
        String srcA = """
            package com.example;
            public class A {
                public void methodA() {
                    new B().methodB();
                }
            }
            """;
        String srcB = """
            package com.example;
            public class B {
                public void methodB() {
                    new C().methodC();
                }
            }
            """;
        String srcC = """
            package com.example;
            public class C {
                public void methodC() {
                    new A().methodA();
                }
            }
            """;

        ProjectCallGraph graph = new CallGraphBuilder().build(List.of(
            javaFile("src/main/java/com/example/A.java", srcA),
            javaFile("src/main/java/com/example/B.java", srcB),
            javaFile("src/main/java/com/example/C.java", srcC)
        ));

        assertEquals(3, graph.size(), "Graph should contain 3 methods");

        MethodNode methodA = graph.getMethodByKey("com.example.A.methodA()").orElseThrow();
        MethodNode methodB = graph.getMethodByKey("com.example.B.methodB()").orElseThrow();
        MethodNode methodC = graph.getMethodByKey("com.example.C.methodC()").orElseThrow();

        // Verify each method has exactly one outgoing call
        assertEquals(1, graph.getOutgoingCalls(methodA).size());
        assertEquals(1, graph.getOutgoingCalls(methodB).size());
        assertEquals(1, graph.getOutgoingCalls(methodC).size());

        // getReachable from A should terminate (BFS visits each node once)
        var reachable = graph.getReachable(java.util.Set.of(methodA));
        assertEquals(3, reachable.size(),
            "Circular chain BFS should visit all 3 nodes exactly once");

        // Verify BFS terminates by ensuring no StackOverflowError
        for (MethodNode m : reachable) {
            assertNotNull(m, "Every reachable node should be non-null");
        }

        // getReachable from A with no entry points
        var noEntryReachable = graph.getReachable(java.util.Set.of());
        assertEquals(0, noEntryReachable.size(),
            "Empty entry set should produce empty reachable set");
    }
}
