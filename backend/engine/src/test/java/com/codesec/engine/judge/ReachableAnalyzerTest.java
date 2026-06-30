package com.codesec.engine.judge;

import com.codesec.engine.model.Finding;
import com.codesec.engine.parser.ParsedFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ReachableAnalyzer")
class ReachableAnalyzerTest {

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
            .title("Test Finding")
            .severity("HIGH")
            .ruleId("test-rule")
            .build();
    }

    private static ReachableAnalyzer analyzer(List<ParsedFile> files) {
        CallGraphBuilder builder = new CallGraphBuilder();
        ProjectCallGraph graph = builder.build(files);
        return new ReachableAnalyzer(graph);
    }

    @Nested
    @DisplayName("classify")
    class Classify {

        @Test
        @DisplayName("reachable method from HTTP entry point returns yes with call path")
        void classify_reachableMethod_returnsYesWithPath() {
            List<String> controller = List.of(
                "package com.example;",
                "import org.springframework.web.bind.annotation.*;",
                "@RestController",
                "public class UserController {",
                "    @GetMapping(\"/users\")",
                "    public String getUsers() {",
                "        new UserService().process(\"test\");",
                "        return \"ok\";",
                "    }",
                "}"
            );
            List<String> service = List.of(
                "package com.example;",
                "public class UserService {",
                "    public void process(String input) {",
                "        new UserDao().query(\"test\");",
                "    }",
                "}"
            );
            List<String> dao = List.of(
                "package com.example;",
                "public class UserDao {",
                "    public void query(String sql) {",
                "        java.sql.Statement stmt = null; stmt.execute(sql);",
                "    }",
                "}"
            );

            ReachableAnalyzer ra = analyzer(List.of(
                javaFile("UserController.java", controller),
                javaFile("UserService.java", service),
                javaFile("UserDao.java", dao)
            ));

            Finding f = finding("src/main/java/com/example/UserDao.java", 4);

            AlgorithmResult result = ra.classify(f);

            AlgorithmResult.Yes yes = assertInstanceOf(AlgorithmResult.Yes.class, result,
                "Should return Yes for reachable method");
            assertTrue(yes.reason().contains("可达路径"), "Reason should mention reachable path");
            assertTrue(yes.reason().contains("getUsers()"), "Path should include entry method");
            assertTrue(yes.reason().contains("process") || yes.reason().contains("process()"),
                "Path should include intermediate method");
            assertTrue(yes.reason().contains("query") || yes.reason().contains("query()"),
                "Path should include vulnerable method");
        }

        @Test
        @DisplayName("unreachable utility method returns no with explanation")
        void classify_unreachableMethod_returnsNo() {
            List<String> controller = List.of(
                "package com.example;",
                "import org.springframework.web.bind.annotation.*;",
                "@RestController",
                "public class AdminController {",
                "    @GetMapping(\"/admin\")",
                "    public String admin() {",
                "        return \"ok\";",
                "    }",
                "}"
            );
            List<String> util = List.of(
                "package com.example;",
                "public class StringUtils {",
                "    public static String format(String input) {",
                "        String secret = \"hardcoded-password\";",
                "        return input.toUpperCase();",
                "    }",
                "}"
            );

            ReachableAnalyzer ra = analyzer(List.of(
                javaFile("AdminController.java", controller),
                javaFile("StringUtils.java", util)
            ));

            Finding f = finding("src/main/java/com/example/StringUtils.java", 4);

            AlgorithmResult result = ra.classify(f);

            AlgorithmResult.No no = assertInstanceOf(AlgorithmResult.No.class, result,
                "Should return No for unreachable utility method");
            assertEquals("未被任何 HTTP 入口调用", no.reason());
        }

        @Test
        @DisplayName("method not found in graph returns undetermined")
        void classify_methodNotFound_returnsUndetermined() {
            List<String> src = List.of(
                "package com.example;",
                "import org.springframework.web.bind.annotation.*;",
                "@RestController",
                "public class FooController {",
                "    @GetMapping(\"/foo\")",
                "    public String foo() { return \"x\"; }",
                "}"
            );

            ReachableAnalyzer ra = analyzer(List.of(
                javaFile("FooController.java", src)
            ));

            Finding f = finding("src/main/java/com/example/NonExistent.java", 1);

            AlgorithmResult result = ra.classify(f);

            AlgorithmResult.Undetermined und = assertInstanceOf(AlgorithmResult.Undetermined.class, result,
                "Should return Undetermined when file not in graph");
            assertTrue(und.reason().contains("NonExistent"),
                "Reason should include the class name");
        }

        @Test
        @DisplayName("project with no HTTP entry points returns undetermined")
        void classify_noEntryPoints_returnsUndetermined() {
            List<String> src = List.of(
                "package com.example;",
                "public class PlainDao {",
                "    public void save() {",
                "        java.sql.Statement stmt = null;",
                "    }",
                "}"
            );

            ReachableAnalyzer ra = analyzer(List.of(
                javaFile("PlainDao.java", src)
            ));

            Finding f = finding("src/main/java/com/example/PlainDao.java", 3);

            AlgorithmResult result = ra.classify(f);

            AlgorithmResult.Undetermined und = assertInstanceOf(AlgorithmResult.Undetermined.class, result,
                "Should return Undetermined when no entry points exist");
            assertTrue(und.reason().contains("HTTP 入口"),
                "Reason should mention no HTTP entry points");
        }

        @Test
        @DisplayName("finding line outside any method range returns undetermined")
        void classify_findingLineOutsideAnyMethod_returnsUndetermined() {
            List<String> src = List.of(
                "package com.example;",
                "import org.springframework.web.bind.annotation.*;",
                "@RestController",
                "public class LineController {",
                "    @GetMapping(\"/line\")",
                "    public String endpoint() {",
                "        return \"ok\";",
                "    }",
                "}"
            );

            ReachableAnalyzer ra = analyzer(List.of(
                javaFile("LineController.java", src)
            ));

            Finding f = finding("src/main/java/com/example/LineController.java", 2);

            AlgorithmResult result = ra.classify(f);

            assertInstanceOf(AlgorithmResult.Undetermined.class, result,
                "Should return Undetermined when line is outside any method range");
        }

        @Test
        @DisplayName("file path without src/main/java prefix parses className correctly")
        void classify_filePathWithoutJavaPrefix_parsesCorrectly() {
            List<String> src = List.of(
                "package com.example;",
                "import org.springframework.web.bind.annotation.*;",
                "@RestController",
                "public class BarController {",
                "    @GetMapping(\"/bar\")",
                "    public String bar() {",
                "        new BarService().work(\"test\");",
                "        return \"ok\";",
                "    }",
                "}"
            );
            List<String> barSvc = List.of(
                "package com.example;",
                "public class BarService {",
                "    public void work(String data) {",
                "        System.out.println(data);",
                "    }",
                "}"
            );

            ReachableAnalyzer ra = analyzer(List.of(
                javaFile("BarController.java", src),
                javaFile("BarService.java", barSvc)
            ));

            Finding f = finding("com/example/BarService.java", 4);

            AlgorithmResult result = ra.classify(f);

            AlgorithmResult.Yes yes = assertInstanceOf(AlgorithmResult.Yes.class, result,
                "Should return Yes when class found via path without prefix");
            assertTrue(yes.reason().contains("可达路径"), "Reason should mention reachable path");
        }

        @Test
        @DisplayName("chained call path includes all intermediate methods in reason")
        void classify_chainedCallPath_reasonIncludesAllMethods() {
            List<String> orchestrator = List.of(
                "package com.example;",
                "import org.springframework.web.bind.annotation.*;",
                "@RestController",
                "public class Orchestrator {",
                "    @GetMapping(\"/chain\")",
                "    public String run() {",
                "        new Processor().step1(\"data\");",
                "        return \"ok\";",
                "    }",
                "}"
            );
            List<String> processor = List.of(
                "package com.example;",
                "public class Processor {",
                "    public void step1(String input) {",
                "        new DataLayer().fetch(\"query\");",
                "    }",
                "}"
            );
            List<String> dataLayer = List.of(
                "package com.example;",
                "public class DataLayer {",
                "    public void fetch(String query) {",
                "        new TargetDao().execute(\"sql\");",
                "    }",
                "}"
            );
            List<String> targetDao = List.of(
                "package com.example;",
                "public class TargetDao {",
                "    public void execute(String sql) {",
                "        java.sql.Statement stmt = null; stmt.execute(sql);",
                "    }",
                "}"
            );

            ReachableAnalyzer ra = analyzer(List.of(
                javaFile("Orchestrator.java", orchestrator),
                javaFile("Processor.java", processor),
                javaFile("DataLayer.java", dataLayer),
                javaFile("TargetDao.java", targetDao)
            ));

            Finding f = finding("src/main/java/com/example/TargetDao.java", 4);

            AlgorithmResult result = ra.classify(f);

            AlgorithmResult.Yes yes = assertInstanceOf(AlgorithmResult.Yes.class, result,
                "Should return Yes for chained reachable method");
            assertTrue(yes.reason().contains("run()"), "Path should include Orchestrator.run()");
            assertTrue(yes.reason().contains("step1()"), "Path should include Processor.step1()");
            assertTrue(yes.reason().contains("fetch()"), "Path should include DataLayer.fetch()");
            assertTrue(yes.reason().contains("execute()"), "Path should include TargetDao.execute()");
        }

        @Test
        @DisplayName("cycle between reachable methods still returns yes")
        void classify_cyclicReachableMethod_stillReturnsYes() {
            List<String> entry = List.of(
                "package com.example;",
                "import org.springframework.web.bind.annotation.*;",
                "@RestController",
                "public class EntryController {",
                "    @GetMapping(\"/cycle\")",
                "    public String handle() {",
                "        new NodeA().callA(\"test\");",
                "        return \"ok\";",
                "    }",
                "}"
            );
            List<String> nodeA = List.of(
                "package com.example;",
                "public class NodeA {",
                "    public void callA(String x) {",
                "        new NodeB().callB(\"data\");",
                "    }",
                "}"
            );
            List<String> nodeB = List.of(
                "package com.example;",
                "public class NodeB {",
                "    public void callB(String y) {",
                "        new NodeA().callA(\"back\");",
                "    }",
                "}"
            );

            ReachableAnalyzer ra = analyzer(List.of(
                javaFile("EntryController.java", entry),
                javaFile("NodeA.java", nodeA),
                javaFile("NodeB.java", nodeB)
            ));

            Finding f = finding("src/main/java/com/example/NodeB.java", 4);

            AlgorithmResult result = ra.classify(f);

            AlgorithmResult.Yes yes = assertInstanceOf(AlgorithmResult.Yes.class, result,
                "Should return Yes despite A↔B cycle");
            assertTrue(yes.reason().contains("可达路径"), "Reason should mention reachable path");
        }
    }
}
