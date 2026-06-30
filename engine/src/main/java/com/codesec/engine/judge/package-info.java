/**
 * Exploitability judgment for SAST findings.
 * <p>
 * Implements three algorithms (isReachable, isUserControllable, hasFrameworkProtection)
 * that enrich raw SAST detections with three-state exploitability classification:
 * {@code EXPLOITABLE}, {@code POTENTIALLY_EXPLOITABLE}, or {@code NOT_EXPLOITABLE}.
 * <p>
 * Core data structures:
 * <ul>
 *   <li>{@link com.codesec.engine.judge.MethodNode} — method identity with annotations and metadata</li>
 *   <li>{@link com.codesec.engine.judge.CallEdge} — directed caller-to-callee relationship</li>
 *   <li>{@link com.codesec.engine.judge.ProjectCallGraph} — in-memory project-wide call graph with BFS traversal</li>
 *   <li>{@link com.codesec.engine.judge.CallGraphBuilder} — JavaParser-based builder extracting methods and calls</li>
 * </ul>
 */
package com.codesec.engine.judge;
