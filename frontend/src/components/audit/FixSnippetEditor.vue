<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import * as monaco from 'monaco-editor'
import { Editor as MonacoEditor } from '@guolao/vue-monaco-editor'
import type { Language } from '@/types/vuln'
import { LANGUAGE_TO_MONACO } from '@/types/vuln'

interface Props {
  modelValue: string
  language: Language
  height?: string
}

const props = withDefaults(defineProps<Props>(), {
  height: '220px',
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const editorRef = ref<monaco.editor.IStandaloneCodeEditor | null>(null)

const monacoLang = computed<string>(() => LANGUAGE_TO_MONACO[props.language])

const options = computed<monaco.editor.IStandaloneEditorConstructionOptions>(() => ({
  readOnly: false,
  fontSize: 13,
  fontFamily: 'JetBrains Mono, SF Mono, Menlo, Consolas, monospace',
  fontLigatures: true,
  lineNumbers: 'on',
  minimap: { enabled: false },
  scrollBeyondLastLine: false,
  smoothScrolling: true,
  cursorBlinking: 'smooth',
  cursorSmoothCaretAnimation: 'on',
  scrollbar: {
    verticalScrollbarSize: 8,
    horizontalScrollbarSize: 8,
  },
  padding: { top: 10, bottom: 10 },
  automaticLayout: true,
  renderWhitespace: 'selection',
  wordWrap: 'off',
  contextmenu: false,
  folding: true,
  bracketPairColorization: { enabled: true },
  tabSize: 2,
  renderLineHighlight: 'gutter',
}))

function handleMount(editor: monaco.editor.IStandaloneCodeEditor): void {
  editorRef.value = editor
  editor.onDidChangeModelContent(() => {
    const value = editor.getValue()
    if (value !== props.modelValue) emit('update:modelValue', value)
  })
}

watch(
  () => monacoLang.value,
  (lang) => {
    if (editorRef.value !== null) {
      const model = editorRef.value.getModel()
      if (model !== null) monaco.editor.setModelLanguage(model, lang)
    }
  },
)

onBeforeUnmount(() => {
  editorRef.value = null
})

onMounted(() => {
  // no-op
})
</script>

<template>
  <div class="cs-fix-editor">
    <MonacoEditor
      :model-value="modelValue"
      :language="monacoLang"
      :theme="'vs-dark'"
      :height="height"
      :options="options"
      @mount="handleMount"
    />
  </div>
</template>

<style scoped>
.cs-fix-editor {
  border: 1px solid var(--cs-border);
  border-radius: var(--cs-radius-md);
  overflow: hidden;
}
</style>
