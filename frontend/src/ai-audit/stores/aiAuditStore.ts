import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { http } from '@/api/client'
import type { AiAnalysisResult, AiAuditProgress } from '@/types/ai-audit'

export const useAiAuditStore = defineStore('aiAudit', () => {
  const aiResults = ref<Map<string, AiAnalysisResult>>(new Map())
  const progressMap = ref<Map<string, AiAuditProgress>>(new Map())
  const isAnalyzing = ref(false)
  const maxConcurrent = 3

  const batchTotal = ref(0)

  const completedCount = computed(() =>
    [...progressMap.value.values()].filter(p => p.status === 'completed' || p.status === 'failed').length
  )

  function getResult(vulnId: string): AiAnalysisResult | undefined {
    return aiResults.value.get(vulnId)
  }

  function getProgress(vulnId: string): AiAuditProgress | undefined {
    return progressMap.value.get(vulnId)
  }

  async function analyzeVuln(vulnId: string): Promise<AiAnalysisResult | null> {
    progressMap.value.set(vulnId, { vulnId, status: 'queued', progress: 0 })
    try {
      progressMap.value.set(vulnId, { vulnId, status: 'analyzing', progress: 50 })
      const resp = await http.post<AiAnalysisResult>(`/ai/analyze/${vulnId}`)
      const result: AiAnalysisResult = {
        ...resp.data,
        vulnId,
        analyzedAt: resp.data.analyzedAt || new Date().toISOString(),
      }
      aiResults.value.set(vulnId, result)
      progressMap.value.set(vulnId, { vulnId, status: 'completed', progress: 100 })
      return result
    } catch (e) {
      progressMap.value.set(vulnId, { vulnId, status: 'failed' })
      return null
    }
  }

  async function batchAnalyze(vulnIds: string[]): Promise<void> {
    isAnalyzing.value = true
    batchTotal.value = vulnIds.length
    const batches: string[][] = []
    for (let i = 0; i < vulnIds.length; i += maxConcurrent) {
      batches.push(vulnIds.slice(i, i + maxConcurrent))
    }
    for (const batch of batches) {
      await Promise.all(batch.map((id) => analyzeVuln(id)))
    }
    isAnalyzing.value = false
  }

  function clearResults(): void {
    aiResults.value.clear()
    progressMap.value.clear()
    isAnalyzing.value = false
    batchTotal.value = 0
  }

  return {
    aiResults,
    progressMap,
    isAnalyzing,
    batchTotal,
    maxConcurrent,
    completedCount,
    getResult,
    getProgress,
    analyzeVuln,
    batchAnalyze,
    clearResults,
  }
})
