import { defineStore } from 'pinia'
import { ref } from 'vue'
import { http } from '@/api/client'
import type { AuditRecord, AuditSubmitPayload } from '@/types/audit'

export const useAuditStore = defineStore('audit', () => {
  const recordsByVuln = ref<Record<string, AuditRecord[]>>({})
  const submitting = ref(false)
  const error = ref<string | null>(null)

  async function fetchHistory(vulnId: string): Promise<AuditRecord[]> {
    try {
      const resp = await http.get<{ items: AuditRecord[]; total: number }>(
        `/vulns/${vulnId}/audits`,
      )
      recordsByVuln.value = {
        ...recordsByVuln.value,
        [vulnId]: resp.data.items,
      }
      return resp.data.items
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to load audit history'
      return []
    }
  }

  async function submitAudit(payload: AuditSubmitPayload): Promise<AuditRecord> {
    submitting.value = true
    error.value = null
    try {
      const resp = await http.post<AuditRecord>('/audits', payload)
      // Prepend the new record to the cached list for that vuln
      const current = recordsByVuln.value[payload.vulnId] ?? []
      recordsByVuln.value = {
        ...recordsByVuln.value,
        [payload.vulnId]: [resp.data, ...current],
      }
      return resp.data
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to submit audit'
      throw e
    } finally {
      submitting.value = false
    }
  }

  function getHistory(vulnId: string): AuditRecord[] {
    return recordsByVuln.value[vulnId] ?? []
  }

  return {
    recordsByVuln,
    submitting,
    error,
    fetchHistory,
    submitAudit,
    getHistory,
  }
})
