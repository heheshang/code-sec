import { ref } from 'vue'

export function useWorkbenchLayout() {
  const leftPanelWidth = ref(320)
  const rightPanelWidth = ref(360)
  const leftCollapsed = ref(false)
  const rightCollapsed = ref(false)
  const minPanelWidth = 200
  const maxPanelWidth = 600

  function toggleLeft() {
    leftCollapsed.value = !leftCollapsed.value
  }

  function toggleRight() {
    rightCollapsed.value = !rightCollapsed.value
  }

  function resizeLeft(delta: number) {
    leftPanelWidth.value = Math.max(
      minPanelWidth,
      Math.min(maxPanelWidth, leftPanelWidth.value + delta),
    )
  }

  function resizeRight(delta: number) {
    rightPanelWidth.value = Math.max(
      minPanelWidth,
      Math.min(maxPanelWidth, rightPanelWidth.value + delta),
    )
  }

  return {
    leftPanelWidth,
    rightPanelWidth,
    leftCollapsed,
    rightCollapsed,
    toggleLeft,
    toggleRight,
    resizeLeft,
    resizeRight,
  }
}
