<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { EditorView, basicSetup } from 'codemirror'
import { EditorState } from '@codemirror/state'
import { Compartment } from '@codemirror/state'
import { syntaxHighlighting, defaultHighlightStyle } from '@codemirror/language'
import { oneDark } from '@codemirror/theme-one-dark'
import { getLanguageExt } from '@/cm-setup'
import type { Language } from '@/types/vuln'

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

const containerRef = ref<HTMLDivElement | null>(null)
let view: EditorView | null = null
const langCompartment = new Compartment()

function buildExtensions() {
  return [
    basicSetup,
    oneDark,
    syntaxHighlighting(defaultHighlightStyle, { fallback: true }),
    langCompartment.of(getLanguageExt(props.language)),
    EditorView.updateListener.of((update) => {
      if (update.docChanged) {
        emit('update:modelValue', update.state.doc.toString())
      }
    }),
    EditorView.theme({
      '&': {
        fontSize: '13px',
        fontFamily: 'JetBrains Mono, SF Mono, Menlo, Consolas, monospace',
        backgroundColor: '#1e1e1e',
      },
      '.cm-scroller': {
        fontFamily: 'JetBrains Mono, SF Mono, Menlo, Consolas, monospace',
      },
      '.cm-gutters': {
        backgroundColor: '#1e1e1e',
        borderRight: '1px solid #2d2d2d',
        color: '#858585',
        fontSize: '12px',
      },
      '.cm-activeLineGutter': {
        backgroundColor: 'transparent',
      },
      '.cm-content': {
        padding: '10px 0',
      },
      '.cm-line': {
        padding: '0 10px',
      },
    }),
  ]
}

onMounted(() => {
  if (containerRef.value === null) return

  const state = EditorState.create({
    doc: props.modelValue,
    extensions: buildExtensions(),
  })

  view = new EditorView({
    state,
    parent: containerRef.value,
  })
})

onBeforeUnmount(() => {
  if (view !== null) {
    view.destroy()
    view = null
  }
})

// Sync external value changes (not user-typed) into the editor
watch(
  () => props.modelValue,
  (newVal) => {
    if (view === null) return
    const cur = view.state.doc.toString()
    if (cur !== newVal) {
      view.dispatch({
        changes: { from: 0, to: cur.length, insert: newVal },
      })
    }
  },
)

// Sync language changes into the editor
watch(
  () => props.language,
  (lang) => {
    if (view === null) return
    view.dispatch({
      effects: langCompartment.reconfigure(getLanguageExt(lang)),
    })
  },
)
</script>

<template>
  <div ref="containerRef" class="cs-cm-fix-editor" :style="{ height }" />
</template>

<style scoped>
.cs-cm-fix-editor {
  border: 1px solid var(--cs-border);
  border-radius: var(--cs-radius-md);
  overflow: hidden;
  background: #1e1e1e;
}
</style>
