import { ref, computed } from 'vue'

export interface GraphNode {
  id: string
  label: string
  type: 'entry' | 'validation' | 'data' | 'trigger'
  filePath?: string
  lineNumber?: number
  severity?: 'critical' | 'high' | 'medium' | 'low'
}

export interface GraphEdge {
  source: string
  target: string
  label?: string
  color?: string
}

export function useGraphInteraction() {
  const selectedNodeId = ref<string | null>(null)
  const highlightedEdges = ref<Set<string>>(new Set())
  const expandedNodes = ref<Set<string>>(new Set())
  const layout = ref<'dagre' | 'bfs'>('dagre')

  function selectNode(nodeId: string | null) {
    selectedNodeId.value = nodeId
  }

  function toggleExpand(nodeId: string) {
    const next = new Set(expandedNodes.value)
    if (next.has(nodeId)) next.delete(nodeId)
    else next.add(nodeId)
    expandedNodes.value = next
  }

  function highlightPath(source: string, target: string, edges: GraphEdge[]) {
    const pathEdges = findPath(source, target, edges)
    highlightedEdges.value = new Set(pathEdges.map((e) => `${e.source}->${e.target}`))
  }

  function clearHighlight() {
    highlightedEdges.value = new Set()
  }

  function findPath(source: string, target: string, edges: GraphEdge[]): GraphEdge[] {
    const visited = new Set<string>()
    const parent = new Map<string, GraphEdge>()

    function dfs(current: string): boolean {
      if (current === target) return true
      visited.add(current)
      for (const edge of edges) {
        if (edge.source === current && !visited.has(edge.target)) {
          parent.set(edge.target, edge)
          if (dfs(edge.target)) return true
        }
      }
      return false
    }

    if (!dfs(source)) return []
    const path: GraphEdge[] = []
    let node = target
    while (node !== source && parent.has(node)) {
      const edge = parent.get(node)!
      path.unshift(edge)
      node = edge.source
    }
    return path
  }

  return {
    selectedNodeId,
    highlightedEdges,
    expandedNodes,
    layout,
    selectNode,
    toggleExpand,
    highlightPath,
    clearHighlight,
  }
}
