import type { Language } from './vuln'

export interface Project {
  id: string
  name: string
  language: Language
  framework: string
  businessLine: string
  owner: string
  repositoryUrl: string
  totalLines: number
  lastScanAt: string
  status: 'active' | 'paused' | 'offline'
  vulnCount: number
  criticalCount: number
  highCount: number
  mediumCount: number
  lowCount: number
  fixRate: number
}
