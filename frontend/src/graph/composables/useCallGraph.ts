import { ref } from 'vue'
import { http } from '@/api/client'

export interface CallGraphNode {
  id: string
  label: string
  type: string
  filePath: string
  lineNumber: number
}

export interface CallGraphEdge {
  sourceId: string
  targetId: string
  type: string
}

export interface CallGraphData {
  nodes: CallGraphNode[]
  edges: CallGraphEdge[]
}

export function useCallGraph(vulnId: string) {
  const data = ref<CallGraphData | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetch() {
    loading.value = true
    error.value = null
    try {
      const resp = await http.get<CallGraphData>(`/cpg/${vulnId}`)
      data.value = resp.data
    } catch (e: unknown) {
      error.value = 'Failed to load call graph'
      data.value = { nodes: [], edges: [] }
    } finally {
      loading.value = false
    }
  }

  return { data, loading, error, fetch }
}
