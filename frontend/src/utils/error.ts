/**
 * Extract a human-readable error message from an unknown error value.
 * Handles Error instances, strings, objects with a message property, and
 * falls back to a generic message for everything else.
 */
export function errMsg(e: unknown): string {
  if (e instanceof Error) return e.message
  if (typeof e === 'string') return e
  if (e && typeof e === 'object' && 'message' in e && typeof (e as Record<string, unknown>).message === 'string') {
    return (e as Record<string, unknown>).message as string
  }
  return 'An unexpected error occurred'
}
