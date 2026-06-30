package com.codesec.engine.judge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ProjectCallGraph")
class ProjectCallGraphTest {

    private static MethodNode methodNode(String fqn, String name, List<String> params) {
        return MethodNode.of(fqn, name, params, "void", List.of());
    }

    private static MethodNode methodNodeWithAnnotations(String fqn, String name, List<String> params, List<String> annotations) {
        return MethodNode.of(fqn, name, params, "void", annotations);
    }

    @Test
    @DisplayName("addMethod then getByKey returns method")
    void addMethod_thenGetByKey_returnsMethod() {
        ProjectCallGraph graph = new ProjectCallGraph();
        MethodNode m = methodNode("com.example.Foo", "bar", List.of("String"));
        graph.addMethod(m);

        MethodNode found = graph.getMethodByKey(m.key()).orElseThrow();
        assertEquals(m, found);
        assertEquals(1, graph.size());
    }

    @Test
    @DisplayName("addMethod with duplicate key silently deduplicates")
    void addMethod_duplicateKey_dedupes() {
        ProjectCallGraph graph = new ProjectCallGraph();
        MethodNode m1 = methodNode("com.example.Foo", "bar", List.of("String"));
        MethodNode m2 = methodNode("com.example.Foo", "bar", List.of("String"));
        graph.addMethod(m1);
        graph.addMethod(m2);

        assertEquals(1, graph.size());
    }

    @Test
    @DisplayName("getMethodsByClass returns only methods from the given FQN")
    void getMethodsByClass_returnsOnlyMatching() {
        ProjectCallGraph graph = new ProjectCallGraph();
        MethodNode a1 = methodNode("com.example.Foo", "bar", List.of());
        MethodNode a2 = methodNode("com.example.Foo", "baz", List.of("int"));
        MethodNode b1 = methodNode("com.example.Other", "qux", List.of());
        graph.addMethod(a1);
        graph.addMethod(a2);
        graph.addMethod(b1);

        List<MethodNode> fooMethods = graph.getMethodsByClass("com.example.Foo");
        assertEquals(2, fooMethods.size());
        assertTrue(fooMethods.contains(a1));
        assertTrue(fooMethods.contains(a2));
        assertFalse(fooMethods.contains(b1));

        List<MethodNode> otherMethods = graph.getMethodsByClass("com.example.Other");
        assertEquals(1, otherMethods.size());
        assertTrue(otherMethods.contains(b1));
    }

    @Test
    @DisplayName("getEntryPoints returns only methods annotated with entry-point annotations")
    void getEntryPoints_returnsRestControllerMethods() {
        ProjectCallGraph graph = new ProjectCallGraph();
        MethodNode rest = methodNodeWithAnnotations("com.example.Ctrl", "handle",
            List.of(), List.of("@RestController"));
        MethodNode getMapping = methodNodeWithAnnotations("com.example.Ctrl", "list",
            List.of(), List.of("@GetMapping"));
        MethodNode plain = methodNode("com.example.Util", "helper", List.of());
        graph.addMethod(rest);
        graph.addMethod(getMapping);
        graph.addMethod(plain);

        List<MethodNode> entries = graph.getEntryPoints();
        assertEquals(2, entries.size());
        assertTrue(entries.contains(rest));
        assertTrue(entries.contains(getMapping));
        assertFalse(entries.contains(plain));
    }

    @Test
    @DisplayName("addCall then getOutgoingCalls returns the edge list")
    void addCall_thenGetOutgoingCalls_returnsList() {
        ProjectCallGraph graph = new ProjectCallGraph();
        MethodNode caller = methodNode("com.example.Foo", "bar", List.of());
        graph.addMethod(caller);

        CallEdge edge = CallEdge.of(caller, "println", "String", 10);
        graph.addCall(edge);

        List<CallEdge> outgoing = graph.getOutgoingCalls(caller);
        assertEquals(1, outgoing.size());
        assertEquals("println", outgoing.get(0).calleeName());
        assertEquals("String", outgoing.get(0).calleeSignature());
        assertEquals(10, outgoing.get(0).callSiteLine());
    }

    @Test
    @DisplayName("getReachable from single entry returns all downstream methods via BFS")
    void getReachable_singleEntry_returnsAllDownstream() {
        ProjectCallGraph graph = new ProjectCallGraph();

        MethodNode entry = methodNode("com.example.Ctrl", "handle", List.of());
        MethodNode service = methodNode("com.example.Service", "process", List.of("String"));
        MethodNode dao = methodNode("com.example.Dao", "query", List.of("String"));
        graph.addMethod(entry);
        graph.addMethod(service);
        graph.addMethod(dao);

        // entry → service("String")
        graph.addCall(CallEdge.of(entry, "process", "String", 1));
        // service → dao("String")
        graph.addCall(CallEdge.of(service, "query", "String", 2));

        Set<MethodNode> reachable = graph.getReachable(Set.of(entry));
        assertEquals(3, reachable.size());
        assertTrue(reachable.contains(entry));
        assertTrue(reachable.contains(service));
        assertTrue(reachable.contains(dao));
    }

    @Test
    @DisplayName("getReachable with A→B→A cycle does not infinite-loop")
    void getReachable_cycle_doesNotInfiniteLoop() {
        ProjectCallGraph graph = new ProjectCallGraph();

        MethodNode a = methodNode("com.example.A", "methodA", List.of());
        MethodNode b = methodNode("com.example.B", "methodB", List.of());
        graph.addMethod(a);
        graph.addMethod(b);

        // A → B
        graph.addCall(CallEdge.of(a, "methodB", "", 1));
        // B → A (cycle)
        graph.addCall(CallEdge.of(b, "methodA", "", 2));

        Set<MethodNode> reachable = graph.getReachable(Set.of(a));
        assertEquals(2, reachable.size());
        assertTrue(reachable.contains(a));
        assertTrue(reachable.contains(b));
    }

    @Test
    @DisplayName("getReachable with A→A self-recursion terminates")
    void getReachable_selfRecursion_terminates() {
        ProjectCallGraph graph = new ProjectCallGraph();

        MethodNode a = methodNode("com.example.A", "methodA", List.of());
        graph.addMethod(a);

        // A → A (self-recursion)
        graph.addCall(CallEdge.of(a, "methodA", "", 1));

        Set<MethodNode> reachable = graph.getReachable(Set.of(a));
        assertEquals(1, reachable.size());
        assertTrue(reachable.contains(a));
    }

    @Test
    @DisplayName("getReachable returns only connected branches, not unrelated siblings")
    void getReachable_unrelatedBranch_notReturned() {
        ProjectCallGraph graph = new ProjectCallGraph();

        MethodNode a = methodNode("com.example.A", "methodA", List.of());
        MethodNode b = methodNode("com.example.B", "methodB", List.of());
        MethodNode c = methodNode("com.example.C", "methodC", List.of());
        graph.addMethod(a);
        graph.addMethod(b);
        graph.addMethod(c);

        // A → B
        graph.addCall(CallEdge.of(a, "methodB", "", 1));
        // No edge from B to C

        Set<MethodNode> reachable = graph.getReachable(Set.of(a));
        assertEquals(2, reachable.size());
        assertTrue(reachable.contains(a));
        assertTrue(reachable.contains(b));
        assertFalse(reachable.contains(c));
    }

    @Test
    @DisplayName("getAllMethods returns unmodifiable list that throws on mutation")
    void getAllMethods_unmodifiable_throwsOnMutation() {
        ProjectCallGraph graph = new ProjectCallGraph();
        graph.addMethod(methodNode("com.example.Foo", "bar", List.of()));

        List<MethodNode> all = graph.getAllMethods();
        assertNotNull(all);
        assertThrows(UnsupportedOperationException.class, () -> all.add(methodNode("x", "y", List.of())));
    }

    @Test
    @DisplayName("Two MethodNodes with same class+name+params are equal by identity")
    void MethodNode_equalityByKey() {
        MethodNode m1 = MethodNode.of("com.example.Foo", "bar", List.of("String"), "void",
            List.of("@Override"), 10, 20, false, true);
        MethodNode m2 = MethodNode.of("com.example.Foo", "bar", List.of("String"), "int",
            List.of(), 30, 40, true, false);

        assertEquals(m1, m2, "MethodNodes with same class+name+params should be equal");
        assertEquals(m1.hashCode(), m2.hashCode(), "Hash codes should match for equal nodes");
        assertEquals(m1.key(), m2.key());
    }

    @Test
    @DisplayName("isEntryPoint detects all 8 entry-point Spring annotations")
    void MethodNode_isEntryPoint() {
        List<String> entryAnnotations = List.of(
            "@RestController", "@Controller", "@RequestMapping",
            "@GetMapping", "@PostMapping", "@PutMapping",
            "@DeleteMapping", "@PatchMapping"
        );

        for (String annotation : entryAnnotations) {
            MethodNode m = MethodNode.of("com.example.Ctrl", "handle", List.of(), "void",
                List.of(annotation));
            assertTrue(m.isEntryPoint(),
                "Method with " + annotation + " should be an entry point");
        }

        MethodNode nonEntry = MethodNode.of("com.example.Util", "helper", List.of(), "void",
            List.of("@Override", "@Deprecated"));
        assertFalse(nonEntry.isEntryPoint(), "Method without entry-point annotations should not be entry point");
    }

    @Test
    @DisplayName("MethodNode.key() produces distinct keys for methods with different parameter lists")
    void MethodNode_key_uniqueness() {
        MethodNode m1 = MethodNode.of("com.example.Foo", "bar", List.of("String"), "void", List.of());
        MethodNode m2 = MethodNode.of("com.example.Foo", "bar", List.of("String", "int"), "void", List.of());
        MethodNode m3 = MethodNode.of("com.example.Foo", "bar", List.of(), "void", List.of());

        String k1 = m1.key();
        String k2 = m2.key();
        String k3 = m3.key();

        assertTrue(k1.endsWith("(String)"));
        assertTrue(k2.endsWith("(String,int)"));
        assertTrue(k3.endsWith("()"));

        assertFalse(k1.equals(k2), "Different params → different keys");
        assertFalse(k1.equals(k3), "Different params → different keys");
        assertFalse(k2.equals(k3), "Different params → different keys");
    }
}
