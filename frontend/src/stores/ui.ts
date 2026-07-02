import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export type Theme = 'light' | 'dark'

interface UiPrefs {
  notifyOnCritical: boolean
  notifyOnRetest: boolean
  dailyDigest: boolean
  scanCompletionToast: boolean
}

const STORAGE_KEY = 'cs.ui.prefs.v1'

const defaultPrefs = (): UiPrefs => ({
  notifyOnCritical: true,
  notifyOnRetest: true,
  dailyDigest: false,
  scanCompletionToast: true,
})

function loadFromStorage(): { theme: Theme; prefs: UiPrefs; sidebarCollapsed: boolean } {
  if (typeof window === 'undefined') {
    return { theme: 'light', prefs: defaultPrefs(), sidebarCollapsed: false }
  }
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    if (!raw) return { theme: 'light', prefs: defaultPrefs(), sidebarCollapsed: false }
    const parsed = JSON.parse(raw) as Partial<{
      theme: Theme
      prefs: Partial<UiPrefs>
      sidebarCollapsed: boolean
    }>
    return {
      theme: parsed.theme === 'dark' ? 'dark' : 'light',
      sidebarCollapsed: Boolean(parsed.sidebarCollapsed),
      prefs: { ...defaultPrefs(), ...(parsed.prefs ?? {}) },
    }
  } catch {
    return { theme: 'light', prefs: defaultPrefs(), sidebarCollapsed: false }
  }
}

export const useUiStore = defineStore('ui', () => {
  const initial = loadFromStorage()
  const theme = ref<Theme>(initial.theme)
  const sidebarCollapsed = ref<boolean>(initial.sidebarCollapsed)
  const prefs = ref<UiPrefs>(initial.prefs)

  function setTheme(t: Theme): void {
    theme.value = t
    if (typeof document !== 'undefined') {
      document.documentElement.classList.toggle('dark', t === 'dark')
    }
  }

  function toggleTheme(): void {
    setTheme(theme.value === 'light' ? 'dark' : 'light')
  }

  function toggleSidebar(): void {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setPref<K extends keyof UiPrefs>(key: K, value: UiPrefs[K]): void {
    prefs.value = { ...prefs.value, [key]: value }
  }

  // Apply the theme to <html> on init
  if (typeof document !== 'undefined') {
    document.documentElement.classList.toggle('dark', theme.value === 'dark')
  }

  // Persist on every change
  watch(
    [theme, sidebarCollapsed, prefs],
    () => {
      if (typeof window === 'undefined') return
      try {
        window.localStorage.setItem(
          STORAGE_KEY,
          JSON.stringify({
            theme: theme.value,
            sidebarCollapsed: sidebarCollapsed.value,
            prefs: prefs.value,
          }),
        )
      } catch {
        // localStorage may be disabled (private mode); ignore.
      }
    },
    { deep: true },
  )

  return {
    theme,
    sidebarCollapsed,
    prefs,
    setTheme,
    toggleTheme,
    toggleSidebar,
    setPref,
  }
})
