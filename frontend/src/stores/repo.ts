import { defineStore } from 'pinia'
import { ref } from 'vue'
import { http } from '@/api/client'
import { ElMessage } from 'element-plus'
import { errMsg } from '@/utils/error'
import type { RepoListItem, RepoResponse, RepoCreateRequest, RepoUpdateRequest, TestConnectionResponse } from '@/types/repo'

export const useRepoStore = defineStore('repo', () => {
  const items = ref<RepoListItem[]>([])
  const total = ref(0)
  const page = ref(1)
  const pageSize = ref(20)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchList(): Promise<void> {
    loading.value = true
    error.value = null
    try {
      const resp = await http.get('/repos', { params: { page: page.value, size: pageSize.value } })
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

  async function getById(id: number): Promise<RepoResponse> {
    const resp = await http.get<RepoResponse>(`/repos/${id}`)
    return resp.data
  }

  async function create(req: RepoCreateRequest): Promise<RepoResponse> {
    const resp = await http.post<RepoResponse>('/repos', req)
    ElMessage.success('Repository created successfully')
    await fetchList()
    return resp.data
  }

  async function update(id: number, req: RepoUpdateRequest): Promise<RepoResponse> {
    const resp = await http.put<RepoResponse>(`/repos/${id}`, req)
    ElMessage.success('Repository updated successfully')
    await fetchList()
    return resp.data
  }

  async function remove(id: number): Promise<void> {
    try {
      await http.delete(`/repos/${id}`)
      ElMessage.success('Repository deleted successfully')
      await fetchList()
    } catch (e: unknown) {
      ElMessage.error(errMsg(e))
    }
  }

  async function testConnection(id: number): Promise<TestConnectionResponse> {
    const resp = await http.post<TestConnectionResponse>(`/repos/${id}/test-connection`)
    return resp.data
  }

  function setPage(p: number): void {
    page.value = p
  }

  return {
    items, total, page, pageSize, loading, error,
    fetchList, getById, create, update, remove, testConnection, setPage,
  }
})
