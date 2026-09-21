export type ParseApiErrorKind = 'parser' | 'backend'

export class ParseApiError extends Error {
  readonly kind: ParseApiErrorKind

  constructor(message: string, kind: ParseApiErrorKind) {
    super(message)
    this.name = 'ParseApiError'
    this.kind = kind
  }
}

export type ParsedQuery = {
  type: 'SELECT'
  columns: Array<Record<string, unknown>>
  from: Record<string, unknown>
  where: Record<string, unknown> | null
}

export async function parseQuery(sql: string): Promise<ParsedQuery> {
  let response: Response
  try {
    response = await fetch('/api/query/parse', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ sql }),
    })
  } catch {
    throw new ParseApiError('Backend unavailable. Make sure the QueryScope API is running.', 'backend')
  }

  const payload = (await response.json().catch(() => null)) as { error?: string } | ParsedQuery | null
  if (!response.ok) {
    const message = payload && 'error' in payload && payload.error
      ? payload.error
      : 'The query could not be parsed.'
    throw new ParseApiError(message, 'parser')
  }
  return payload as ParsedQuery
}
