export type RepoPlatform = 'gitlab' | 'github' | 'gitee'
export type RepoStatus = 'active' | 'inactive' | 'error'

export interface RepoListItem {
  id: number
  name: string
  platform: RepoPlatform
  url: string
  businessLine: string
  status: RepoStatus
  createdAt: string
}

export interface RepoResponse {
  id: number
  name: string
  platform: RepoPlatform
  url: string
  defaultBranch: string
  businessLine: string
  status: RepoStatus
  gitlabProjectId: number | null
  hasToken: boolean
  createdAt: string
  updatedAt: string
}

export interface RepoCreateRequest {
  name: string
  platform?: RepoPlatform
  url: string
  accessToken?: string
  webhookSecret?: string
  defaultBranch?: string
  businessLine?: string
  gitlabProjectId?: number
}

export interface RepoUpdateRequest {
  name?: string
  url?: string
  accessToken?: string
  defaultBranch?: string
  businessLine?: string
  status?: RepoStatus
}

export interface TestConnectionResponse {
  ok: boolean
  error?: string
  branches: string[]
}
