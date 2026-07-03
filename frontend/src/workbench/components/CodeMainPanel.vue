<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'

const props = defineProps<{
  code?: string
  language?: string
  highlightLines?: [number, number]
}>()

const editorRef = ref<HTMLDivElement | null>(null)
const codeLines = ref<string[]>([])

watch(
  () => props.code,
  (val) => {
    codeLines.value = (val || '').split('\n')
  },
  { immediate: true },
)
</script>

<template>
  <div class="cs-code-panel">
    <div class="cs-code-panel-toolbar">
      <span class="cs-code-panel-lang">{{ language || 'text' }}</span>
      <el-button size="small" text>
        <el-icon><CopyDocument /></el-icon>
      </el-button>
    </div>

    <div ref="editorRef" class="cs-code-panel-editor">
      <div class="cs-code-gutter">
        <div v-for="(_, i) in codeLines" :key="i" class="cs-code-line-num">
          {{ i + 1 }}
        </div>
      </div>
      <div class="cs-code-content">
        <div
          v-for="(line, i) in codeLines"
          :key="i"
          class="cs-code-line"
          :class="{
            highlight:
              highlightLines &&
              i + 1 >= highlightLines[0] &&
              i + 1 <= highlightLines[1],
          }"
        >
          {{ line || ' ' }}
        </div>
      </div>
    </div>

    <div class="cs-code-panel-footer">
      <span class="cs-code-panel-info">
        {{ codeLines.length }} lines
        <template v-if="highlightLines">
          · L{{ highlightLines[0] }}-L{{ highlightLines[1] }}
        </template>
      </span>
    </div>
  </div>
</template>

<style scoped>
.cs-code-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #0F1117;
  border-radius: var(--cs-radius-md);
  overflow: hidden;
}

.cs-code-panel-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  background: #1A1C24;
  border-bottom: 1px solid #2A2D3A;
}

.cs-code-panel-lang {
  font-size: 11px;
  font-weight: 600;
  color: #8C8C8C;
  text-transform: uppercase;
}

.cs-code-panel-editor {
  flex: 1;
  display: flex;
  overflow: auto;
  font-family: var(--cs-font-mono);
  font-size: 13px;
  line-height: 1.6;
}

.cs-code-gutter {
  display: flex;
  flex-direction: column;
  padding: 8px 0;
  background: #14161E;
  border-right: 1px solid #2A2D3A;
  user-select: none;
}

.cs-code-line-num {
  text-align: right;
  padding: 0 10px;
  color: #4A4D5C;
  font-size: 12px;
}

.cs-code-content {
  flex: 1;
  padding: 8px 0;
  overflow-x: auto;
}

.cs-code-line {
  padding: 0 16px;
  color: #D4D4D8;
  white-space: pre;
}

.cs-code-line.highlight {
  background: rgba(207, 19, 34, 0.15);
  border-left: 3px solid var(--cs-severity-critical);
}

.cs-code-panel-footer {
  padding: 4px 12px;
  background: #1A1C24;
  border-top: 1px solid #2A2D3A;
}

.cs-code-panel-info {
  font-size: 11px;
  color: #6A6D7C;
  font-family: var(--cs-font-mono);
}
</style>
