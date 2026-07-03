<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { VueFlow, useVueFlow, ConnectionLineType, MarkerType, type Node, type Edge } from '@vue-flow/core'
import { MiniMap } from '@vue-flow/minimap'
import { Controls } from '@vue-flow/controls'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import { useCallGraph } from '../composables/useCallGraph'
import GraphToolbar from './GraphToolbar.vue'
import dagre from 'dagre'

const props = defineProps<{ vulnId: string }>()

const emit = defineEmits<{
  nodeClick: [nodeId: string]
}>()

const { data, loading, error, fetch } = useCallGraph(props.vulnId)

const graphRef = ref<HTMLDivElement | null>(null)

const { fitView, zoomIn, zoomOut } = useVueFlow({ id: 'cpg-flow' })

function isEntryPoint(nodeType: string): boolean {
  const tags = ['@RequestMapping', '@GetMapping', '@PostMapping', '@PutMapping', '@DeleteMapping', '@PatchMapping', '@RestController', '@Controller']
  return tags.some(t => nodeType.includes(t))
}

const flowNodes = computed<Node[]>(() => {
  if (!data.value) return []
  const g = new dagre.graphlib.Graph()
  g.setDefaultEdgeLabel(() => ({}))
  g.setGraph({ rankdir: 'LR', nodesep: 60, ranksep: 100 })

  const nodes: Node[] = data.value.nodes.map(n => {
    const entry = isEntryPoint(n.type)
    const w = Math.max(160, n.label.length * 8 + 40)
    g.setNode(n.id, { width: w, height: 44 })
    return {
      id: n.id,
      position: { x: 0, y: 0 },
      data: { label: n.label, sub: n.type, entry, filePath: n.filePath, line: n.lineNumber },
      style: {
        background: entry ? '#5B47E0' : '#1a1a2e',
        color: '#fff',
        border: entry ? '2px solid #7c6af0' : '1px solid #333',
        borderRadius: '8px',
        padding: '8px 14px',
        fontSize: '12px',
        fontFamily: 'JetBrains Mono, SF Mono, monospace',
        width: `${w}px`,
      },
    }
  })

  data.value.edges.forEach(e => g.setEdge(e.sourceId, e.targetId))
  dagre.layout(g)

  for (const n of nodes) {
    const p = g.node(n.id)
    n.position = { x: p.x - p.width / 2, y: p.y - p.height / 2 }
  }
  return nodes
})

const flowEdges = computed<Edge[]>(() => {
  if (!data.value) return []
  return data.value.edges.map((e, i) => ({
    id: `e-${i}`,
    source: e.sourceId,
    target: e.targetId,
    type: 'smoothstep',
    animated: true,
    style: { stroke: '#555', strokeWidth: 1.5 },
    markerEnd: { type: MarkerType.ArrowClosed, color: '#555' },
  }))
})

const hasData = computed(() => flowNodes.value.length > 0)

function handleNodeClick(event: { node: Node }) {
  emit('nodeClick', event.node.id)
}

function handleFitView() {
  fitView({ padding: 0.2 })
}

function handleExportPng() {
  const el = graphRef.value?.querySelector('.vue-flow__viewport') as HTMLElement
  if (!el) return
  import('html-to-image').then(m => {
    m.toPng(el, { backgroundColor: '#0d0d1a' }).then(url => {
      const a = document.createElement('a')
      a.href = url
      a.download = `cpg-${props.vulnId}.png`
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
    })
  })
}

onMounted(() => { if (props.vulnId) fetch() })

watch(() => props.vulnId, id => { if (id) fetch() })
</script>

<template>
  <div ref="graphRef" class="cs-call-graph">
    <div v-if="loading" class="cs-call-graph-placeholder">
      <div class="cs-call-graph-spinner" />
      <p>Loading call graph…</p>
    </div>

    <div v-else-if="error" class="cs-call-graph-placeholder">
      <el-empty description="Call graph not available" />
    </div>

    <div v-else-if="!hasData" class="cs-call-graph-placeholder">
      <el-empty description="No call graph data for this vulnerability" />
    </div>

    <template v-else>
      <VueFlow
        id="cpg-flow"
        :nodes="flowNodes"
        :edges="flowEdges"
        :connection-line-type="ConnectionLineType.SmoothStep"
        :default-viewport="{ x: 0, y: 0, zoom: 0.8 }"
        :min-zoom="0.1"
        :max-zoom="4"
        fit-view-on-init
        @node-click="handleNodeClick"
      >
        <MiniMap
          :node-color="(node: Node) => (node.data as any)?.entry ? '#5B47E0' : '#1a1a2e'"
          mask-color="rgba(0,0,0,0.3)"
        />
        <Controls :show-interactive="false" />
      </VueFlow>

      <div class="cs-call-graph-legend">
        <div class="cs-legend-item"><span class="cs-legend-dot entry" /> Entry</div>
        <div class="cs-legend-item"><span class="cs-legend-dot internal" /> Internal</div>
      </div>

      <GraphToolbar
        @zoom-in="zoomIn"
        @zoom-out="zoomOut"
        @fit-view="handleFitView"
        @export-png="handleExportPng"
      />
    </template>
  </div>
</template>

<style scoped>
.cs-call-graph {
  position: relative;
  width: 100%;
  height: 400px;
  background: #0d0d1a;
  border: 1px solid var(--cs-border);
  border-radius: var(--cs-radius-md);
  overflow: hidden;
}

.cs-call-graph-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 8px;
  color: var(--cs-text-tertiary);
  font-size: 13px;
}

.cs-call-graph-spinner {
  width: 24px;
  height: 24px;
  border: 2px solid var(--cs-border);
  border-top-color: var(--cs-primary);
  border-radius: 50%;
  animation: cs-spin 0.8s linear infinite;
}

@keyframes cs-spin {
  to { transform: rotate(360deg); }
}

.cs-call-graph-legend {
  position: absolute;
  bottom: 8px;
  left: 8px;
  display: flex;
  gap: 12px;
  padding: 6px 10px;
  background: rgba(13, 13, 26, 0.85);
  border: 1px solid var(--cs-border);
  border-radius: var(--cs-radius-sm);
  font-size: 11px;
  color: #ccc;
  z-index: 10;
}

.cs-legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.cs-legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.cs-legend-dot.entry { background: #5B47E0; }
.cs-legend-dot.internal { background: #1a1a2e; border: 1px solid #333; }
</style>
