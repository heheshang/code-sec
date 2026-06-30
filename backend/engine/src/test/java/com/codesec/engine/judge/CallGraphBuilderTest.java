package com.codesec.engine.judge;

import com.codesec.engine.parser.ParsedFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CallGraphBuilder")
class CallGraphBuilderTest {

    private static final CallGraphBuilder BUILDER = new CallGraphBuilder();

    private static ParsedFile javaFile(String name, String sourceCode) {
        return new ParsedFile(Path.of(name), "java", sourceCode, null);
    }

    private static ParsedFile nonJavaFile(String name) {
        return new ParsedFile(Path.of(name), "python", "print('hello')", null);
    }

    @Test
    @DisplayName("build with empty file list returns empty graph")
    void build_emptyList_returnsEmptyGraph() {
        ProjectCallGraph graph = BUILDER.build(List.of());
        assertEquals(0, graph.size());
    }

    @Test
    @DisplayName("build with a single-method class adds the method to the graph")
    void build_singleMethodClass_addsMethod() {
        String src = """
            package com.example;
            public class UserDao {
                public User findById(String id) { return null; }
            }
            """;

        ProjectCallGraph graph = BUILDER.build(List.of(javaFile("UserDao.java", src)));

        assertEquals(1, graph.size());
        MethodNode m = graph.getMethodByKey("com.example.UserDao.findById(String)").orElseThrow();
        assertEquals("com.example.UserDao", m.className());
        assertEquals("findById", m.methodName());
        assertEquals(List.of("String"), m.parameterTypes());
        assertEquals("User", m.returnType());
        assertTrue(m.isPublic());
    }

    @Test
    @DisplayName("build creates call edges when one method calls another")
    void build_methodWithCalls_createsCallEdges() {
        String srcCaller = """
            package com.example;
            public class Caller {
                public void callMethod() {
                    new Callee().targetMethod("hello");
                }
            }
            """;
        String srcCallee = """
            package com.example;
            public class Callee {
                public void targetMethod(String msg) {}
            }
            """;

        ProjectCallGraph graph = BUILDER.build(List.of(
            javaFile("Caller.java", srcCaller),
            javaFile("Callee.java", srcCallee)
        ));

        assertTrue(graph.size() >= 2, "Should have at least 2 methods");

        MethodNode caller = graph.getMethodByKey("com.example.Caller.callMethod()").orElseThrow();
        List<CallEdge> outgoing = graph.getOutgoingCalls(caller);
        assertFalse(outgoing.isEmpty(), "Caller should have outgoing calls");

        CallEdge edge = outgoing.get(0);
        assertEquals("targetMethod", edge.calleeName());
        assertEquals("String", edge.calleeSignature());
        assertTrue(edge.callSiteLine() > 0, "Call site line should be positive");
    }

    @Test
    @DisplayName("build creates distinct nodes for methods with same name but different parameters")
    void build_methodOverloading_distinctNodes() {
        String src = """
            package com.example;
            public class Overloader {
                public void process(String s) {}
                public void process(int i) {}
                public void process(String s, int i) {}
            }
            """;

        ProjectCallGraph graph = BUILDER.build(List.of(javaFile("Overloader.java", src)));

        assertEquals(3, graph.size());

        String key1 = "com.example.Overloader.process(String)";
        String key2 = "com.example.Overloader.process(int)";
        String key3 = "com.example.Overloader.process(String,int)";

        assertTrue(graph.getMethodByKey(key1).isPresent());
        assertTrue(graph.getMethodByKey(key2).isPresent());
        assertTrue(graph.getMethodByKey(key3).isPresent());
    }

    @Test
    @DisplayName("build keeps both parent and child methods when overriding")
    void build_methodOverriding_keepsBothMethods() {
        String srcParent = """
            package com.example;
            public class Parent {
                public void greet() { System.out.println("parent"); }
            }
            """;
        String srcChild = """
            package com.example;
            public class Child extends Parent {
                @Override
                public void greet() { System.out.println("child"); }
            }
            """;

        ProjectCallGraph graph = BUILDER.build(List.of(
            javaFile("Parent.java", srcParent),
            javaFile("Child.java", srcChild)
        ));

        MethodNode parentMethod = graph.getMethodByKey("com.example.Parent.greet()").orElseThrow();
        MethodNode childMethod = graph.getMethodByKey("com.example.Child.greet()").orElseThrow();

        assertEquals("com.example.Parent", parentMethod.className());
        assertEquals("com.example.Child", childMethod.className());
        assertTrue(childMethod.annotations().contains("@Override"));
    }

    @Test
    @DisplayName("build identifies @GetMapping method as entry point")
    void build_restController_identifiedAsEntryPoint() {
        String src = """
            package com.example;
            public class UserController {
                @GetMapping("/users")
                public void getUsers() {}
            }
            """;

        ProjectCallGraph graph = BUILDER.build(List.of(javaFile("UserController.java", src)));

        List<MethodNode> entries = graph.getEntryPoints();
        assertEquals(1, entries.size(), "Should have one entry point");
        assertEquals("getUsers", entries.get(0).methodName());
    }

    @Test
    @DisplayName("build skips files with parse errors and continues processing others")
    void build_parseError_continuesToNextFile() {
        String badSrc = "class Broken { this is not valid Java }";
        String goodSrc = """
            package com.example;
            public class Good {
                public void ok() {}
            }
            """;

        ProjectCallGraph graph = BUILDER.build(List.of(
            javaFile("Broken.java", badSrc),
            javaFile("Good.java", goodSrc)
        ));

        assertEquals(1, graph.size(), "Should succeed on good file despite parse error");
        assertTrue(graph.getMethodByKey("com.example.Good.ok()").isPresent());
    }

    @Test
    @DisplayName("build skips non-Java files")
    void build_nonJavaFile_skipped() {
        String src = """
            package com.example;
            public class Foo {
                public void bar() {}
            }
            """;

        ProjectCallGraph graph = BUILDER.build(List.of(
            nonJavaFile("script.py"),
            javaFile("Foo.java", src)
        ));

        assertEquals(1, graph.size(), "Only the Java file should be processed");
    }

    @Test
    @DisplayName("build extracts parameter types correctly")
    void build_extractsParameterTypes() {
        String src = """
            package com.example;
            public class Processor {
                public void foo(String s, int i, List<String> items) {}
            }
            """;

        ProjectCallGraph graph = BUILDER.build(List.of(javaFile("Processor.java", src)));

        MethodNode m = graph.getMethodByKey("com.example.Processor.foo(String,int,List<String>)").orElseThrow();
        assertEquals(3, m.parameterTypes().size());
        assertEquals("String", m.parameterTypes().get(0));
        assertEquals("int", m.parameterTypes().get(1));
        assertEquals("List<String>", m.parameterTypes().get(2));
    }

    @Test
    @DisplayName("build extracts return type correctly")
    void build_extractsReturnType() {
        String src = """
            package com.example;
            public class Factory {
                public String getName() { return "test"; }
                public void doSomething() {}
                public int getCount() { return 0; }
            }
            """;

        ProjectCallGraph graph = BUILDER.build(List.of(javaFile("Factory.java", src)));

        MethodNode m1 = graph.getMethodByKey("com.example.Factory.getName()").orElseThrow();
        assertEquals("String", m1.returnType());

        MethodNode m2 = graph.getMethodByKey("com.example.Factory.doSomething()").orElseThrow();
        assertEquals("void", m2.returnType());

        MethodNode m3 = graph.getMethodByKey("com.example.Factory.getCount()").orElseThrow();
        assertEquals("int", m3.returnType());
    }

    @Test
    @DisplayName("build qualifies nested class names correctly")
    void build_nestedClass_qualifiesName() {
        String src = """
            package com.example;
            public class Outer {
                public class Inner {
                    public void nestedMethod() {}
                }
                public void outerMethod() {}
            }
            """;

        ProjectCallGraph graph = BUILDER.build(List.of(javaFile("Outer.java", src)));

        assertNotNull(graph.getMethodByKey("com.example.Outer.outerMethod()").orElse(null),
            "Outer class method should use fully qualified name");
        assertNotNull(graph.getMethodByKey("com.example.Outer.Inner.nestedMethod()").orElse(null),
            "Nested class method should include outer class in name");
    }
}
