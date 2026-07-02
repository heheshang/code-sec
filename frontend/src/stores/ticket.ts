import { defineStore } from 'pinia'
import { ref } from 'vue'
import { http } from '@/api/client'
import { ElMessage } from 'element-plus'
import { errMsg } from '@/utils/error'
import type { TicketResponse, TicketHistoryItem, TicketTransitionRequest, TicketStatus } from '@/types/ticket'

export const useTicketStore = defineStore('ticket', () => {
  const items = ref<TicketResponse[]>([])
  const total = ref(0)
  const page = ref(1)
  const pageSize = ref(20)
  const statusFilter = ref<TicketStatus | ''>('')
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchList(): Promise<void> {
    loading.value = true
    error.value = null
    try {
      const params: Record<string, unknown> = { page: page.value, size: pageSize.value }
      if (statusFilter.value) params.status = statusFilter.value
      const resp = await http.get('/tickets', { params })
      items.value = resp.data.items
      total.value = resp.data.total
      page.value = resp.data.page
      pageSize.value = resp.data.size ?? resp.data.pageSize ?? 20
    } catch (e: unknown) {
      error.value = errMsg(e)
    } finally {
      loading.value = false
    }
  }

  async function getById(id: number): Promise<TicketResponse> {
    const resp = await http.get<TicketResponse>(`/tickets/${id}`)
    return resp.data
  }

  async function getHistory(id: number): Promise<TicketHistoryItem[]> {
    const resp = await http.get<TicketHistoryItem[]>(`/tickets/${id}/history`)
    return resp.data
  }

  async function transition(id: number, req: TicketTransitionRequest): Promise<TicketResponse> {
    const resp = await http.post<TicketResponse>(`/tickets/${id}/transition`, req)
    await fetchList()
    return resp.data
  }

  async function assign(id: number, req: TicketTransitionRequest): Promise<TicketResponse> {
    const resp = await http.post<TicketResponse>(`/tickets/${id}/assign`, req)
    await fetchList()
    return resp.data
  }

  function setPage(p: number): void {
    page.value = p
  }

  function setStatusFilter(s: TicketStatus | ''): void {
    statusFilter.value = s
    page.value = 1
  }

  return {
    items, total, page, pageSize, statusFilter, loading, error,
    fetchList, getById, getHistory, transition, assign, setPage, setStatusFilter,
  }
})
