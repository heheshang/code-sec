import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { errMsg } from '@/utils/error'
import type { AxiosInstance } from 'axios'

export interface FetchParams {
  page?: number
  pageSize?: number
  [key: string]: unknown
}

export function useCrudStore<T extends { id: number | string }>(
  httpClient: AxiosInstance,
  basePath: string,
  key: string = 'items',
) {
  const items = ref<T[]>([])
  const total = ref(0)
  const page = ref(1)
  const pageSize = ref(20)
  const loading = ref(false)
  const error = ref<string | null>(null)

  const hasMore = computed(() => items.value.length < total.value)

  async function fetchItems(params?: FetchParams): Promise<void> {
    loading.value = true
    error.value = null
    try {
      const res = await httpClient.get(basePath, {
        params: {
          page: page.value,
          page_size: pageSize.value,
          ...params,
        },
      })
      const data = res.data
      if (Array.isArray(data)) {
        items.value = data as T[]
        total.value = data.length
      } else if (data[key]) {
        items.value = data[key] as T[]
        total.value = data.total ?? data[key].length
      } else {
        items.value = []
        total.value = 0
      }
    } catch (e: unknown) {
      error.value = errMsg(e)
      ElMessage.error(errMsg(e))
    } finally {
      loading.value = false
    }
  }

  async function fetchById(id: number | string): Promise<T | null> {
    try {
      const res = await httpClient.get(`${basePath}/${id}`)
      return res.data as T
    } catch (e: unknown) {
      ElMessage.error(errMsg(e))
      return null
    }
  }

  async function create(payload: Partial<T>): Promise<boolean> {
    try {
      await httpClient.post(basePath, payload)
      ElMessage.success('Created successfully')
      await fetchItems()
      return true
    } catch (e: unknown) {
      ElMessage.error(errMsg(e))
      return false
    }
  }

  async function update(id: number | string, payload: Partial<T>): Promise<boolean> {
    try {
      await httpClient.put(`${basePath}/${id}`, payload)
      ElMessage.success('Updated successfully')
      await fetchItems()
      return true
    } catch (e: unknown) {
      ElMessage.error(errMsg(e))
      return false
    }
  }

  async function remove(id: number | string): Promise<boolean> {
    try {
      await httpClient.delete(`${basePath}/${id}`)
      ElMessage.success('Deleted successfully')
      await fetchItems()
      return true
    } catch (e: unknown) {
      ElMessage.error(errMsg(e))
      return false
    }
  }

  function setPage(p: number): void {
    page.value = p
    fetchItems()
  }

  function setPageSize(s: number): void {
    pageSize.value = s
    page.value = 1
    fetchItems()
  }

  function reset(): void {
    items.value = []
    total.value = 0
    page.value = 1
    pageSize.value = 20
    error.value = null
  }

  return {
    items,
    total,
    page,
    pageSize,
    loading,
    error,
    hasMore,
    fetchItems,
    fetchById,
    create,
    update,
    remove,
    setPage,
    setPageSize,
    reset,
  }
}
