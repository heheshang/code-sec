package com.codesec.engine.judge;

import com.codesec.engine.model.Finding;
import com.codesec.engine.parser.ParsedFile;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("InputControllabilityAnalyzer")
class InputControllabilityAnalyzerTest {

    private static final CallGraphBuilder BUILDER = new CallGraphBuilder();

    private static ParsedFile javaFile(String name, String sourceCode) {
        return new ParsedFile(Path.of(name), "java", sourceCode, null);
    }

    /** Helper: builds graph + parsed-files map, creates the analyzer. */
    private InputControllabilityAnalyzer analyzerFor(Map<String, String> fileNameToSource) {
        List<ParsedFile> files = fileNameToSource.entrySet().stream()
            .map(e -> javaFile(e.getKey(), e.getValue()))
            .toList();
        ProjectCallGraph graph = BUILDER.build(files);
        Map<String, ParsedFile> parsedFileMap = new HashMap<>();
        for (ParsedFile pf : files) {
            parsedFileMap.put(pf.path().toString(), pf);
        }
        return new InputControllabilityAnalyzer(graph, parsedFileMap);
    }

    /** Shorthand: single-file setup. */
    private InputControllabilityAnalyzer analyzerFor(String fileName, String sourceCode) {
        return analyzerFor(Map.of(fileName, sourceCode));
    }

    private static Finding finding(String filePath, int lineStart, int lineEnd) {
        return Finding.builder()
            .filePath(filePath)
            .lineStart(lineStart)
            .lineEnd(lineEnd)
            .build();
    }

    // ── classify tests ──────────────────────────────────────────────

    @Nested
    @DisplayName("classify")
    class ClassifyTests {

        @Test
        @DisplayName("@RequestParam annotation on parameter → YES")
        void classify_requestParamAnnotation_saysYes() {
            // line 6 is the method signature with @RequestParam String id
            String src = """
                package com.example;
                import org.springframework.web.bind.annotation.*;
                @RestController
                public class UserController {
                    @GetMapping("/user")
                    public String getUser(@RequestParam String id) {
                        return userDao.findById(id);
                    }
                }
                """;
            String filePath = "src/main/java/com/example/UserController.java";
            InputControllabilityAnalyzer analyzer = analyzerFor(filePath, src);
            Finding f = finding(filePath, 7, 7);

            AlgorithmResult result = analyzer.classify(f);

            assertTrue(result instanceof AlgorithmResult.Yes,
                "Expected AlgorithmResult.Yes, got " + result.getClass().getSimpleName());
            String reason = ((AlgorithmResult.Yes) result).reason();
            assertTrue(reason.contains("RequestParam"),
                "Reason should mention @RequestParam: " + reason);
        }

        @Test
        @DisplayName("HttpServletRequest.getParameter() in method body → YES (conservative)")
        void classify_servletRequestCallInBody_saysYes() {
            // line 6: method; line 7: String id = request.getParameter("id")
            String src = """
                package com.example;
                import javax.servlet.http.HttpServletRequest;
                @RestController
                public class UserController {
                    @GetMapping("/user")
                    public String getUser(HttpServletRequest request) {
                        String id = request.getParameter("id");
                        return userDao.findById(id);
                    }
                }
                """;
            String filePath = "src/main/java/com/example/UserController.java";
            InputControllabilityAnalyzer analyzer = analyzerFor(filePath, src);
            Finding f = finding(filePath, 8, 8);

            AlgorithmResult result = analyzer.classify(f);

            assertTrue(result instanceof AlgorithmResult.Yes,
                "Expected AlgorithmResult.Yes, got " + result.getClass().getSimpleName());
            String reason = ((AlgorithmResult.Yes) result).reason();
            assertTrue(reason.contains("getParameter") || reason.contains("用户输入源"),
                "Reason should mention taint source: " + reason);
        }

        @Test
        @DisplayName("No user input source → UNDETERMINED")
        void classify_noTaintSource_saysUndetermined() {
            String src = """
                package com.example;
                public class UserService {
                    public String processData(String input) {
                        return input.toUpperCase();
                    }
                }
                """;
            String filePath = "src/main/java/com/example/UserService.java";
            InputControllabilityAnalyzer analyzer = analyzerFor(filePath, src);
            Finding f = finding(filePath, 4, 4);

            AlgorithmResult result = analyzer.classify(f);

            assertTrue(result instanceof AlgorithmResult.Undetermined,
                "Expected Undetermined, got " + result.getClass().getSimpleName());
        }

        @Test
        @DisplayName("@RequestBody annotation on parameter → YES")
        void classify_requestBodyAnnotation_saysYes() {
            String src = """
                package com.example;
                import org.springframework.web.bind.annotation.*;
                @RestController
                public class UserController {
                    @PostMapping("/user")
                    public String createUser(@RequestBody UserData data) {
                        return userDao.insert(data);
                    }
                }
                """;
            String filePath = "src/main/java/com/example/UserController.java";
            InputControllabilityAnalyzer analyzer = analyzerFor(filePath, src);
            Finding f = finding(filePath, 7, 7);

            AlgorithmResult result = analyzer.classify(f);

            assertTrue(result instanceof AlgorithmResult.Yes);
            String reason = ((AlgorithmResult.Yes) result).reason();
            assertTrue(reason.contains("RequestBody"),
                "Reason should mention @RequestBody: " + reason);
        }

        @Test
        @DisplayName("@PathVariable annotation on parameter → YES")
        void classify_pathVariable_saysYes() {
            String src = """
                package com.example;
                import org.springframework.web.bind.annotation.*;
                @RestController
                public class UserController {
                    @GetMapping("/user/{id}")
                    public String getUser(@PathVariable String id) {
                        return userDao.findById(id);
                    }
                }
                """;
            String filePath = "src/main/java/com/example/UserController.java";
            InputControllabilityAnalyzer analyzer = analyzerFor(filePath, src);
            Finding f = finding(filePath, 7, 7);

            AlgorithmResult result = analyzer.classify(f);

            assertTrue(result instanceof AlgorithmResult.Yes);
            String reason = ((AlgorithmResult.Yes) result).reason();
            assertTrue(reason.contains("PathVariable"),
                "Reason should mention @PathVariable: " + reason);
        }

        @Test
        @DisplayName("Purely internal utility method → UNDETERMINED")
        void classify_internalOnlyMethod_saysUndetermined() {
            String src = """
                package com.example;
                public class MathUtils {
                    public static int add(int a, int b) {
                        return a + b;
                    }
                }
                """;
            String filePath = "src/main/java/com/example/MathUtils.java";
            InputControllabilityAnalyzer analyzer = analyzerFor(filePath, src);
            Finding f = finding(filePath, 4, 4);

            AlgorithmResult result = analyzer.classify(f);

            assertTrue(result instanceof AlgorithmResult.Undetermined,
                "Internal-only method should be undetermined: " + result);
        }

        @Test
        @DisplayName("Both annotation taint and body taint → YES (annotation wins)")
        void classify_combinedTaint_saysYes() {
            // Method has @RequestParam AND body contains request.getParameter()
            // but the algorithm short-circuits on annotation first
            String src = """
                package com.example;
                import javax.servlet.http.HttpServletRequest;
                import org.springframework.web.bind.annotation.*;
                @RestController
                public class UserController {
                    @GetMapping("/user")
                    public String getUser(@RequestParam String userId,
                                          HttpServletRequest request) {
                        String extra = request.getParameter("extra");
                        return userDao.findById(userId);
                    }
                }
                """;
            String filePath = "src/main/java/com/example/UserController.java";
            InputControllabilityAnalyzer analyzer = analyzerFor(filePath, src);
            Finding f = finding(filePath, 10, 10);

            AlgorithmResult result = analyzer.classify(f);

            assertTrue(result instanceof AlgorithmResult.Yes);
            String reason = ((AlgorithmResult.Yes) result).reason();
            assertTrue(reason.contains("userId") || reason.contains("RequestParam"),
                "Should detect @RequestParam annotation: " + reason);
        }

        @Test
        @DisplayName("File not in parsed files map → UNDETERMINED")
        void classify_methodNotFound_saysUndetermined() {
            String src = """
                package com.example;
                public class SomeClass {
                    public void someMethod() {}
                }
                """;
            String filePath = "src/main/java/com/example/KnownFile.java";
            InputControllabilityAnalyzer analyzer = analyzerFor(filePath, src);

            // Pass a finding for a DIFFERENT file that is not in the map
            Finding f = finding("src/main/java/com/example/UnknownFile.java", 3, 3);

            AlgorithmResult result = analyzer.classify(f);

            assertTrue(result instanceof AlgorithmResult.Undetermined);
        }
    }

    // ── TaintTracker tests ───────────────────────────────────────────

    @Nested
    @DisplayName("TaintTracker")
    class TaintTrackerTests {

        @Test
        @DisplayName("findTaintSources: multiple sources in one file → returns all sorted by line")
        void findTaintSources_multipleInFile_returnsAll() {
            String src = """
                package com.example;
                import javax.servlet.http.HttpServletRequest;
                import org.springframework.web.bind.annotation.*;
                @RestController
                public class MultiController {
                    @GetMapping("/data")
                    public void handle(HttpServletRequest req,
                                       @RequestBody String body,
                                       @RequestParam String query) {
                        String id = req.getParameter("id");
                        String token = req.getHeader("Authorization");
                        process(id, token, body, query);
                    }
                    private void process(String... args) {}
                }
                """;

            TaintTracker tracker = new TaintTracker(javaFile("MultiController.java", src));
            List<TaintTracker.TaintOccurrence> occurrences = tracker.findTaintSources();

            assertFalse(occurrences.isEmpty(), "Should find multiple taint sources");
            long paramCount = occurrences.stream()
                .filter(o -> o.source() == TaintSource.HTTP_REQUEST_GET_PARAMETER)
                .count();
            long headerCount = occurrences.stream()
                .filter(o -> o.source() == TaintSource.HTTP_REQUEST_GET_HEADER)
                .count();
            long bodyCount = occurrences.stream()
                .filter(o -> o.source() == TaintSource.SPRING_REQUEST_BODY)
                .count();
            long reqParamCount = occurrences.stream()
                .filter(o -> o.source() == TaintSource.SPRING_REQUEST_PARAM)
                .count();

            assertTrue(paramCount >= 1, "Should find getParameter: " + occurrences);
            assertTrue(headerCount >= 1, "Should find getHeader: " + occurrences);
            assertTrue(bodyCount >= 1, "Should find @RequestBody: " + occurrences);
            assertTrue(reqParamCount >= 1, "Should find @RequestParam: " + occurrences);

            // Verify sorted by line
            for (int i = 1; i < occurrences.size(); i++) {
                assertTrue(occurrences.get(i - 1).line() <= occurrences.get(i).line(),
                    "Occurrences should be sorted by line");
            }
        }

        @Test
        @DisplayName("findTaintedParameters: @RequestParam → correct index mapping")
        void findTaintedParameters_requestParamAnnotation_returnsMapping() {
            // line 6: public String getUser(@RequestParam String id)
            String src = """
                package com.example;
                import org.springframework.web.bind.annotation.*;
                @RestController
                public class UserController {
                    @GetMapping("/user")
                    public String getUser(@RequestParam String id) {
                        return userDao.findById(id);
                    }
                }
                """;

            TaintTracker tracker = new TaintTracker(javaFile("UserController.java", src));
            MethodNode method = MethodNode.of(
                "com.example.UserController", "getUser",
                List.of("String"), "String",
                List.of("@GetMapping"), 6, 8, false, true
            );

            Map<Integer, TaintSource> tainted = tracker.findTaintedParameters(method);

            assertEquals(1, tainted.size(), "One parameter should be tainted");
            assertTrue(tainted.containsKey(0), "Parameter index 0 should be tainted");
            assertEquals(TaintSource.SPRING_REQUEST_PARAM, tainted.get(0));
        }

        @Test
        @DisplayName("findTaintedParameters: internal HttpServletRequest assignment → NOT marked (v1)")
        void findTaintedParameters_internalAssignment_doesNotMark() {
            // line 6: handle(@RequestParam String safe, String unsafe)
            // Body has request.getParameter(), but findTaintedParameters only
            // returns annotation-based taint in v1
            String src = """
                package com.example;
                import javax.servlet.http.HttpServletRequest;
                import org.springframework.web.bind.annotation.*;
                @RestController
                public class MixedController {
                    @GetMapping("/process")
                    public void handle(@RequestParam String safe,
                                       String unsafe,
                                       HttpServletRequest request) {
                        String id = request.getParameter("id");
                        process(safe, unsafe, id);
                    }
                    private void process(String... args) {}
                }
                """;

            TaintTracker tracker = new TaintTracker(javaFile("MixedController.java", src));
            MethodNode method = MethodNode.of(
                "com.example.MixedController", "handle",
                List.of("String", "String", "HttpServletRequest"), "void",
                List.of("@GetMapping"), 7, 14, false, true
            );

            Map<Integer, TaintSource> tainted = tracker.findTaintedParameters(method);

            // Only parameter 0 should be tainted (via @RequestParam)
            assertEquals(1, tainted.size(),
                "Only annotation-tainted parameter should be marked. Got: " + tainted);
            assertTrue(tainted.containsKey(0), "Parameter 0 (@RequestParam) should be tainted");
            assertFalse(tainted.containsKey(1),
                "Parameter 1 (unsafe) should NOT be tainted — only annotations count in v1");
            assertEquals(TaintSource.SPRING_REQUEST_PARAM, tainted.get(0));
        }

        @Test
        @DisplayName("hasBodyTaint: returns true when body contains getParameter call")
        void hasBodyTaint_withServletRequestCall_returnsTrue() {
            String src = """
                package com.example;
                import javax.servlet.http.HttpServletRequest;
                public class ServiceController {
                    public void process(HttpServletRequest request) {
                        String id = request.getParameter("id");
                        execute(id);
                    }
                    private void execute(String id) {}
                }
                """;

            TaintTracker tracker = new TaintTracker(javaFile("ServiceController.java", src));
            MethodNode method = MethodNode.of(
                "com.example.ServiceController", "process",
                List.of("HttpServletRequest"), "void",
                List.of(), 4, 7, false, true
            );

            assertTrue(tracker.hasBodyTaint(method),
                "Method body with getParameter() should be detected as tainted");
        }

        @Test
        @DisplayName("hasBodyTaint: returns false when body has no request methods")
        void hasBodyTaint_noServletRequestCall_returnsFalse() {
            String src = """
                package com.example;
                public class CleanService {
                    public void process(String input) {
                        String trimmed = input.trim();
                        execute(trimmed);
                    }
                    private void execute(String s) {}
                }
                """;

            TaintTracker tracker = new TaintTracker(javaFile("CleanService.java", src));
            MethodNode method = MethodNode.of(
                "com.example.CleanService", "process",
                List.of("String"), "void",
                List.of(), 3, 6, false, true
            );

            assertFalse(tracker.hasBodyTaint(method),
                "Clean method body should not be flagged as tainted");
        }

        @Test
        @DisplayName("findTaintedParameters: @CookieValue annotation → correct mapping")
        void findTaintedParameters_cookieValue_returnsMapping() {
            String src = """
                package com.example;
                import org.springframework.web.bind.annotation.*;
                @RestController
                public class SessionController {
                    @GetMapping("/session")
                    public String check(@CookieValue String sessionId) {
                        return validate(sessionId);
                    }
                }
                """;

            TaintTracker tracker = new TaintTracker(javaFile("SessionController.java", src));
            MethodNode method = MethodNode.of(
                "com.example.SessionController", "check",
                List.of("String"), "String",
                List.of("@GetMapping"), 5, 7, false, true
            );

            Map<Integer, TaintSource> tainted = tracker.findTaintedParameters(method);

            assertEquals(1, tainted.size());
            assertEquals(TaintSource.SPRING_REQUEST_COOKIE, tainted.get(0));
        }

        @Test
        @DisplayName("findTaintedParameters: multiple tainted parameters → all mapped")
        void findTaintedParameters_multipleTaintedParameters_allMapped() {
            String src = """
                package com.example;
                import org.springframework.web.bind.annotation.*;
                @RestController
                public class SearchController {
                    @GetMapping("/search")
                    public String search(@RequestParam String q,
                                         @RequestHeader("X-Tenant") String tenant) {
                        return execute(q, tenant);
                    }
                }
                """;

            TaintTracker tracker = new TaintTracker(javaFile("SearchController.java", src));
            MethodNode method = MethodNode.of(
                "com.example.SearchController", "search",
                List.of("String", "String"), "String",
                List.of("@GetMapping"), 5, 8, false, true
            );

            Map<Integer, TaintSource> tainted = tracker.findTaintedParameters(method);

            assertEquals(2, tainted.size());
            assertEquals(TaintSource.SPRING_REQUEST_PARAM, tainted.get(0));
            assertEquals(TaintSource.SPRING_REQUEST_HEADER, tainted.get(1));
        }
    }

    // ── filePathToClassFqn tests ─────────────────────────────────────

    @Nested
    @DisplayName("filePathToClassFqn")
    class FilePathToClassFqnTests {

        @Test
        @DisplayName("standard src/main/java path → FQN")
        void standardSourcePath_convertsCorrectly() {
            String fqn = InputControllabilityAnalyzer.filePathToClassFqn(
                "src/main/java/com/example/UserController.java");
            assertEquals("com.example.UserController", fqn);
        }

        @Test
        @DisplayName("src/test/java path → FQN")
        void testSourcePath_convertsCorrectly() {
            String fqn = InputControllabilityAnalyzer.filePathToClassFqn(
                "src/test/java/com/example/UserControllerTest.java");
            assertEquals("com.example.UserControllerTest", fqn);
        }

        @Test
        @DisplayName("no standard prefix → treats whole path as package")
        void noStandardPrefix_usesWholePath() {
            String fqn = InputControllabilityAnalyzer.filePathToClassFqn(
                "com/example/UserController.java");
            assertEquals("com.example.UserController", fqn);
        }
    }
}
