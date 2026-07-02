export type TicketStatus =
  | 'pending_scan'
  | 'pending_audit'
  | 'confirmed'
  | 'false_positive'
  | 'pending_fix'
  | 'pending_retest'
  | 'fixing'
  | 'closed'

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
  pending_scan: 'Pending scan',
  pending_audit: 'Pending audit',
  confirmed: 'Confirmed',
  false_positive: 'False positive',
  pending_fix: 'Pending fix',
  pending_retest: 'Pending retest',
  fixing: 'Fixing',
  closed: 'Closed',
}
