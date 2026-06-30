import { http, HttpResponse, delay } from 'msw'
import type { Vuln, VulnStatus, Exploitability, Severity } from '@/types/vuln'
import type { AuditRecord, AuditSubmitPayload } from '@/types/audit'
import { SEVERITY_ORDER, type PaginatedResult } from '@/types/vuln'
import { projects, vulns, auditRecords } from './data'

/**
 * In-memory mutable state for the MSW handlers.
 * This is what the workbench reads from and writes to during a session.
 */
const state: {
  vulns: Vuln[]
  audits: AuditRecord[]
} = {
  vulns: vulns.map((v) => ({ ...v })),
  audits: auditRecords.map((a) => ({ ...a })),
}

function paginate<T>(items: T[], page: number, pageSize: number): PaginatedResult<T> {
  const start = (page - 1) * pageSize
  const slice = items.slice(start, start + pageSize)
  return {
    total: items.length,
    page,
    pageSize,
    items: slice,
  }
}

interface VulnListRequestBody {
  projectId?: string
  severity?: Severity[]
  status?: VulnStatus[]
  exploitability?: Exploitability[]
  keyword?: string
  page?: number
  pageSize?: number
  sortBy?: 'severity' | 'discoveredAt' | 'projectId'
  sortOrder?: 'asc' | 'desc'
}

function applyVulnQuery(body: VulnListRequestBody): Vuln[] {
  let out = state.vulns.slice()

  if (body.projectId) {
    out = out.filter((v) => v.projectId === body.projectId)
  }
  if (body.severity && body.severity.length > 0) {
    const set = new Set(body.severity)
    out = out.filter((v) => set.has(v.severity))
  }
  if (body.status && body.status.length > 0) {
    const set = new Set(body.status)
    out = out.filter((v) => set.has(v.status))
  }
  if (body.exploitability && body.exploitability.length > 0) {
    const set = new Set(body.exploitability)
    out = out.filter((v) => set.has(v.exploitability))
  }
  if (body.keyword && body.keyword.trim().length > 0) {
    const kw = body.keyword.trim().toLowerCase()
    out = out.filter(
      (v) =>
        v.title.toLowerCase().includes(kw) ||
        v.filePath.toLowerCase().includes(kw) ||
        v.ruleId.toLowerCase().includes(kw) ||
        v.cwe.toLowerCase().includes(kw),
    )
  }

  const sortBy = body.sortBy ?? 'severity'
  const sortOrder = body.sortOrder ?? 'asc'
  out.sort((a, b) => {
    let cmp = 0
    if (sortBy === 'severity') {
      cmp = SEVERITY_ORDER[a.severity] - SEVERITY_ORDER[b.severity]
    } else if (sortBy === 'discoveredAt') {
      cmp = a.discoveredAt.localeCompare(b.discoveredAt)
    } else {
      cmp = a.projectId.localeCompare(b.projectId)
    }
    return sortOrder === 'asc' ? cmp : -cmp
  })

  return out
}

function resultingStatusFor(action: AuditSubmitPayload['action']): VulnStatus {
  switch (action) {
    case 'confirm':
      return 'confirmed'
    case 'false_positive':
      return 'false_positive'
    case 'need_retest':
      return 'pending_retest'
  }
}

function resultingExploitFor(
  action: AuditSubmitPayload['action'],
): Exploitability | null {
  if (action === 'false_positive') return 'NOT_EXPLOITABLE'
  return null
}

function nextAuditId(): string {
  const max = state.audits
    .map((a) => Number(a.id.replace('audit-', '')))
    .reduce((a, b) => Math.max(a, b), 0)
  return `audit-${String(max + 1).padStart(3, '0')}`
}

export const handlers = [
  http.get('/api/v1/projects', async () => {
    await delay(150)
    return HttpResponse.json({ items: projects, total: projects.length })
  }),

  http.get('/api/v1/dashboard/stats', async () => {
    await delay(180)
    const total = state.vulns.length
    const critical = state.vulns.filter((v) => v.severity === 'critical').length
    const open = state.vulns.filter(
      (v) => v.status !== 'closed' && v.status !== 'false_positive',
    ).length
    const fixedThisWeek = state.vulns.filter((v) => {
      if (v.closedAt === null) return false
      const sevenDaysAgo = Date.now() - 7 * 24 * 60 * 60 * 1000
      return new Date(v.closedAt).getTime() > sevenDaysAgo
    }).length
    const fixRate = state.vulns.length > 0
      ? state.vulns.filter((v) => v.status === 'closed' || v.status === 'false_positive').length /
        state.vulns.length
      : 0

    const severityCounts: Record<Severity, number> = {
      critical: 0, high: 0, medium: 0, low: 0, info: 0,
    }
    state.vulns.forEach((v) => {
      severityCounts[v.severity] += 1
    })

    const trend: { date: string; opened: number; closed: number }[] = []
    for (let i = 13; i >= 0; i -= 1) {
      const d = new Date()
      d.setDate(d.getDate() - i)
      const dateStr = d.toISOString().slice(0, 10)
      const opened = state.vulns.filter((v) => v.discoveredAt.slice(0, 10) === dateStr).length
      const closed = state.vulns.filter((v) => v.closedAt !== null && v.closedAt.slice(0, 10) === dateStr).length
      trend.push({ date: dateStr, opened, closed })
    }

    return HttpResponse.json({
      total,
      critical,
      open,
      fixedThisWeek,
      fixRate,
      severityCounts,
      trend,
      projectCount: projects.length,
    })
  }),

  http.get('/api/v1/vulns', async ({ request }) => {
    await delay(200)
    const url = new URL(request.url)
    const body: VulnListRequestBody = {
      projectId: url.searchParams.get('projectId') ?? undefined,
      keyword: url.searchParams.get('keyword') ?? undefined,
      page: Number(url.searchParams.get('page') ?? 1),
      pageSize: Number(url.searchParams.get('pageSize') ?? 20),
      sortBy: (url.searchParams.get('sortBy') as VulnListRequestBody['sortBy']) ?? 'severity',
      sortOrder: (url.searchParams.get('sortOrder') as 'asc' | 'desc') ?? 'asc',
    }
    const severityCsv = url.searchParams.get('severity')
    if (severityCsv) body.severity = severityCsv.split(',') as Severity[]
    const statusCsv = url.searchParams.get('status')
    if (statusCsv) body.status = statusCsv.split(',') as VulnStatus[]
    const exploitCsv = url.searchParams.get('exploitability')
    if (exploitCsv) body.exploitability = exploitCsv.split(',') as Exploitability[]

    const filtered = applyVulnQuery(body)
    return HttpResponse.json(paginate(filtered, body.page ?? 1, body.pageSize ?? 20))
  }),

  http.get('/api/v1/vulns/:vulnId', async ({ params }) => {
    await delay(150)
    const vuln = state.vulns.find((v) => v.id === params.vulnId)
    if (!vuln) {
      return HttpResponse.json({ message: 'vuln not found' }, { status: 404 })
    }
    return HttpResponse.json(vuln)
  }),

  http.get('/api/v1/vulns/:vulnId/audits', async ({ params }) => {
    await delay(150)
    const items = state.audits
      .filter((a) => a.vulnId === params.vulnId)
      .sort((a, b) => b.auditedAt.localeCompare(a.auditedAt))
    return HttpResponse.json({ items, total: items.length })
  }),

  http.post('/api/v1/audits', async ({ request }) => {
    await delay(280)
    const payload = (await request.json()) as AuditSubmitPayload
    if (!payload.vulnId) {
      return HttpResponse.json({ message: 'vulnId required' }, { status: 400 })
    }
    const vuln = state.vulns.find((v) => v.id === payload.vulnId)
    if (!vuln) {
      return HttpResponse.json({ message: 'vuln not found' }, { status: 404 })
    }
    const id = nextAuditId()
    const resultingStatus = resultingStatusFor(payload.action)
    const resultingExploitability = resultingExploitFor(payload.action)
    const record: AuditRecord = {
      id,
      vulnId: payload.vulnId,
      auditorId: 'user-current-auditor',
      auditorName: 'You (current auditor)',
      action: payload.action,
      exploitCondition: payload.exploitCondition,
      pocContent: payload.pocContent,
      pocAttachments: payload.pocAttachments,
      impactScope: payload.impactScope,
      businessScenario: payload.businessScenario,
      fixSuggestion: payload.fixSuggestion,
      fixCodeSnippet: payload.fixCodeSnippet,
      fixLanguage: payload.fixLanguage,
      resultingStatus,
      resultingExploitability,
      auditDurationSeconds: Math.max(60, Math.floor(payload.exploitCondition.length / 4)),
      auditedAt: new Date().toISOString(),
    }
    state.audits.unshift(record)

    // Apply resulting state on the vuln
    vuln.status = resultingStatus
    if (resultingExploitability !== null) {
      vuln.exploitability = resultingExploitability
    }
    if (resultingStatus === 'fixing' || resultingStatus === 'pending_retest') {
      vuln.assignee = 'zhang.jing'
    }
    if (resultingStatus === 'closed') {
      vuln.closedAt = new Date().toISOString()
    }

    return HttpResponse.json(record, { status: 201 })
  }),

  http.get('/api/v1/reports', async () => {
    await delay(160)
    return HttpResponse.json({
      items: [
        {
          id: 'rpt-monthly',
          name: 'Monthly Security Report',
          description: 'Aggregate vulnerability trends, top-10 risky projects, and SLA compliance for the current month.',
          frequency: 'monthly',
          lastGeneratedAt: '2026-06-01T02:00:00Z',
        },
        {
          id: 'rpt-project',
          name: 'Project Audit Report',
          description: 'Per-project deep-dive: all open and recently closed findings, with full audit trail.',
          frequency: 'on-demand',
          lastGeneratedAt: null,
        },
        {
          id: 'rpt-sla',
          name: 'SLA Compliance Report',
          description: 'Time-to-fix distribution, overdue tickets, breach counts by severity.',
          frequency: 'weekly',
          lastGeneratedAt: '2026-06-23T02:00:00Z',
        },
        {
          id: 'rpt-rules',
          name: 'Rules Engine Execution',
          description: 'Engine hit-rate, false-positive rate, and per-rule performance over the last 30 days.',
          frequency: 'weekly',
          lastGeneratedAt: '2026-06-23T02:00:00Z',
        },
      ],
    })
  }),
]
