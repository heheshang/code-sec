<script setup lang="ts">
defineProps<{
  nodes: Array<{
    id: string
    label: string
    layer: 'source' | 'validation' | 'data' | 'sink'
    severity?: string
  }>
}>()
</script>

<template>
  <div class="cs-taint-flow">
    <div class="cs-taint-layer" v-for="layer in ['source', 'validation', 'data', 'sink']" :key="layer">
      <div class="cs-taint-layer-label">{{ layer }}</div>
      <div class="cs-taint-layer-nodes">
        <div
          v-for="node in nodes.filter(n => n.layer === layer)"
          :key="node.id"
          class="cs-taint-node"
          :class="node.severity"
        >
          {{ node.label }}
        </div>
        <div v-if="nodes.filter(n => n.layer === layer).length === 0" class="cs-taint-empty">
          No {{ layer }} nodes
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cs-taint-flow {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 12px;
}

.cs-taint-layer {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.cs-taint-layer-label {
  font-size: 11px;
  font-weight: 700;
  color: var(--cs-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.cs-taint-layer-nodes {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.cs-taint-node {
  padding: 4px 10px;
  border-radius: var(--cs-radius-sm);
  font-size: 12px;
  font-family: var(--cs-font-mono);
  border: 1px solid var(--cs-border);
  background: var(--cs-bg-elevated);
}

.cs-taint-node.critical {
  border-color: var(--cs-severity-critical);
  background: var(--cs-severity-critical-bg);
  color: var(--cs-severity-critical);
}

.cs-taint-node.high {
  border-color: var(--cs-severity-high);
  background: var(--cs-severity-high-bg);
  color: var(--cs-severity-high);
}

.cs-taint-empty {
  font-size: 12px;
  color: var(--cs-text-disabled);
  font-style: italic;
}
</style>
