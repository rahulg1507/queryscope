export type QueryApiErrorKind = 'parser' | 'execution' | 'backend'
export type JoinStrategy = 'NESTED_LOOP' | 'HASH'
export type ScanStrategy = 'TABLE' | 'INDEX'
export type ExecutionMode = 'AUTO' | 'MANUAL'

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
  groupBy?: Array<Record<string, unknown>>
}

export type QueryResult = {
  columns: Array<{ name: string; type: string }>
  rows: unknown[][]
  rowCount: number
  metrics: { rowsScanned: number; rowsReturned: number }
  executionPlan: ExecutionPlanNode
  optimization?: OptimizationInfo | null
}

export type OptimizationTrace = { rule: string; decision: string; reason: string }
export type OptimizationInfo = {
  mode: ExecutionMode
  originalPlan: ExecutionPlanNode
  optimizedPlan: ExecutionPlanNode
  rulesApplied: OptimizationTrace[]
}

export type ExecutionPlanNode = {
  type: string
  details: Record<string, unknown>
  inputRows: number
  outputRows: number
  children: ExecutionPlanNode[]
}

async function postSql(path: string, sql: string, errorKind: 'parser' | 'execution', options?: { mode?: ExecutionMode; joinStrategy?: JoinStrategy; scanStrategy?: ScanStrategy }) {
  try {
    const response = await fetch(path, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ sql, ...options }),
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

export async function executeQuery(sql: string, joinStrategy: JoinStrategy = 'NESTED_LOOP', scanStrategy: ScanStrategy = 'TABLE', mode: ExecutionMode = 'AUTO'): Promise<QueryResult> {
  return await postSql('/api/query/execute', sql, 'execution', { joinStrategy, scanStrategy, mode }) as QueryResult
}

export type SchemaColumn = { name: string; type: string }
export type SchemaIndex = { name: string; column: string }
export type SchemaTable = { name: string; columns: SchemaColumn[]; indexes: SchemaIndex[] }
export type SchemaResponse = { tables: SchemaTable[] }

async function schemaRequest(path: string, init?: RequestInit): Promise<SchemaResponse> {
  try {
    const response = await fetch(path, init)
    const payload = (await response.json().catch(() => null)) as SchemaResponse & { error?: string } | null
    if (!response.ok) throw new ParseApiError(payload?.error ?? 'The schema request failed.', 'execution')
    return payload as SchemaResponse
  } catch (error) {
    if (error instanceof ParseApiError) throw error
    throw new ParseApiError('Backend unavailable. Make sure the QueryScope API is running.', 'backend')
  }
}

export async function getSchema(): Promise<SchemaResponse> {
  return schemaRequest('/api/schema')
}

export async function createIndex(name: string, table: string, column: string): Promise<SchemaResponse> {
  return schemaRequest('/api/schema/indexes', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, table, column }),
  })
}
