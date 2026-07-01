export type TicketStatus =
  | 'open'
  | 'in_progress'
  | 'fixed'
  | 'closed'
  | 'waived'
  | 'rejected'
  | 'retest'

export interface TicketResponse {
  id: number
  vulnId: number
  projectId: number
  status: TicketStatus
  severity: string
  assigneeId: number | null
  assigneeName: string | null
  deadline: string | null
  createdAt: string
  updatedAt: string
}

export interface TicketHistoryItem {
  id: number
  fromStatus: string
  toStatus: string
  comment: string | null
  operatorId: number
  operatedAt: string
}

export interface TicketTransitionRequest {
  toStatus: TicketStatus
  comment?: string
  assigneeId?: number
}

export const TICKET_STATUS_LABEL: Record<TicketStatus, string> = {
  open: 'Open',
  in_progress: 'In Progress',
  fixed: 'Fixed',
  closed: 'Closed',
  waived: 'Waived',
  rejected: 'Rejected',
  retest: 'Retest',
}
