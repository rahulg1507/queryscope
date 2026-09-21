export type QueryApiErrorKind = 'parser' | 'execution' | 'backend'
export type JoinStrategy = 'NESTED_LOOP' | 'HASH'

export class ParseApiError extends Error {
  readonly kind: QueryApiErrorKind

  constructor(message: string, kind: QueryApiErrorKind) {
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

export type QueryResult = {
  columns: Array<{ name: string; type: string }>
  rows: unknown[][]
  rowCount: number
  metrics: { rowsScanned: number; rowsReturned: number }
  executionPlan: ExecutionPlanNode
}

export type ExecutionPlanNode = {
  type: string
  details: Record<string, unknown>
  inputRows: number
  outputRows: number
  children: ExecutionPlanNode[]
}

async function postSql(path: string, sql: string, errorKind: 'parser' | 'execution', joinStrategy?: JoinStrategy) {
  try {
    const response = await fetch(path, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(joinStrategy ? { sql, joinStrategy } : { sql }),
    })
    const payload = (await response.json().catch(() => null)) as { error?: string } | null
    if (!response.ok) {
      throw new ParseApiError(payload?.error ?? 'The query request failed.', errorKind)
    }
    return payload
  } catch (error) {
    if (error instanceof ParseApiError) throw error
    throw new ParseApiError('Backend unavailable. Make sure the QueryScope API is running.', 'backend')
  }
}

export async function parseQuery(sql: string): Promise<ParsedQuery> {
  return await postSql('/api/query/parse', sql, 'parser') as ParsedQuery
}

export async function executeQuery(sql: string, joinStrategy: JoinStrategy = 'NESTED_LOOP'): Promise<QueryResult> {
  return await postSql('/api/query/execute', sql, 'execution', joinStrategy) as QueryResult
}
