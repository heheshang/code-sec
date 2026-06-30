import { onMounted, onUnmounted, type Ref } from 'vue'

/**
 * Registers a global keyboard shortcut.
 * v1: ⌘K / Ctrl+K to trigger global search.
 */
export function useGlobalShortcut(
  combo: { key: string; metaKey?: boolean; ctrlKey?: boolean },
  callback: () => void,
  active: Ref<boolean>,
): void {
  function handler(e: KeyboardEvent): void {
    if (!active.value) return

    const metaMatch = combo.metaKey === undefined || e.metaKey === combo.metaKey
    const ctrlMatch = combo.ctrlKey === undefined || e.ctrlKey === combo.ctrlKey
    const keyMatch = e.key.toLowerCase() === combo.key.toLowerCase()

    if (metaMatch && ctrlMatch && keyMatch) {
      e.preventDefault()
      callback()
    }
  }

  onMounted(() => {
    window.addEventListener('keydown', handler)
  })

  onUnmounted(() => {
    window.removeEventListener('keydown', handler)
  })
}
