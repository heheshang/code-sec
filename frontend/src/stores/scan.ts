import { defineStore } from 'pinia'
import { ref } from 'vue'
import { http } from '@/api/client'
import { ElMessage } from 'element-plus'
import { errMsg } from '@/utils/error'
import type { ScanListItem, ScanTaskResponse, ScanCreateRequest } from '@/types/scan'

export const useScanStore = defineStore('scan', () => {
  const items = ref<ScanListItem[]>([])
  const total = ref(0)
  const page = ref(1)
  const pageSize = ref(20)
  const repoId = ref<number | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchList(rId?: number): Promise<void> {
    loading.value = true
    error.value = null
    if (rId !== undefined) repoId.value = rId
    try {
      const params: Record<string, unknown> = { page: page.value, size: pageSize.value }
      if (repoId.value) params.repoId = repoId.value
      const resp = await http.get('/scans', { params })
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

  async function getById(id: number): Promise<ScanTaskResponse> {
    const resp = await http.get<ScanTaskResponse>(`/scans/${id}`)
    return resp.data
  }

  async function create(req: ScanCreateRequest): Promise<{ scanId: number }> {
    const resp = await http.post<{ scanId: number }>('/scans', req)
    ElMessage.success('Scan created successfully')
    await fetchList()
    return resp.data
  }

  async function cancel(id: number): Promise<void> {
    try {
      await http.delete(`/scans/${id}`)
      ElMessage.success('Scan cancelled')
      await fetchList()
    } catch (e: unknown) {
      ElMessage.error(errMsg(e))
    }
  }

  function setPage(p: number): void {
    page.value = p
  }

  function setRepoId(rId: number | null): void {
    repoId.value = rId
    page.value = 1
  }

  return {
    items, total, page, pageSize, repoId, loading, error,
    fetchList, getById, create, cancel, setPage, setRepoId,
  }
})
