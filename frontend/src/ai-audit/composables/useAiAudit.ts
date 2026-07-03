import { computed } from 'vue'
import { useAiAuditStore } from '../stores/aiAuditStore'
import type { AiVerdict } from '@/types/ai-audit'

export function useAiAudit(vulnId?: string) {
  const store = useAiAuditStore()

  const result = computed(() => (vulnId ? store.getResult(vulnId) : undefined))
  const progress = computed(() => (vulnId ? store.getProgress(vulnId) : undefined))

  function confidenceColor(confidence: number): string {
    if (confidence >= 0.8) return 'var(--cs-color-accent)'
    if (confidence >= 0.5) return 'var(--cs-status-retest)'
    return 'var(--cs-severity-critical)'
  }

  function verdictIcon(verdict: AiVerdict): string {
    switch (verdict) {
      case 'exploitable':
        return 'WarningFilled'
      case 'false_positive':
        return 'CircleCheckFilled'
      case 'suspicious':
        return 'QuestionFilled'
    }
  }

  return { result, progress, confidenceColor, verdictIcon }
}
