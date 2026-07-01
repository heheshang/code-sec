export type ScanMode = 'full' | 'mr'
export type ScanStatus = 'pending' | 'running' | 'completed' | 'failed' | 'cancelled'

export interface ScanListItem {
  id: number
  repoId: number
  branch: string
  status: ScanStatus
  mode: ScanMode
  startedAt: string | null
  finishedAt: string | null
}

export interface ScanTaskResponse {
  id: number
  repoId: number
  branch: string
  commitSha: string
  status: ScanStatus
  engine: string
  mode: ScanMode
  errorMessage: string | null
  findingsCount: number
  startedAt: string | null
  finishedAt: string | null
}

export interface ScanCreateRequest {
  repoId: number
  mode?: ScanMode
  branch?: string
  commitSha?: string
  engines?: string[]
}

export interface ScanResponse {
  scanId: number
}
