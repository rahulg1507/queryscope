import { useEffect, useState, type FormEvent } from 'react'
import { Database, Plus, RefreshCw } from 'lucide-react'
import type { SchemaResponse, StatisticsResponse } from '../api/query'

type SchemaIndexPanelProps = {
  schema: SchemaResponse | null
  isLoading: boolean
  error: string
  isCreating: boolean
  onRefresh: () => void | Promise<unknown>
  onCreateIndex: (name: string, table: string, column: string) => void | boolean | Promise<void | boolean>
  statistics?: StatisticsResponse | null
  onUseTable?: (name: string) => void
  success?: string
}

export function SchemaIndexPanel({ schema, statistics, isLoading, error, success, isCreating, onRefresh, onCreateIndex, onUseTable }: SchemaIndexPanelProps) {
  const firstTable = schema?.tables[0]
  const [tableName, setTableName] = useState(firstTable?.name ?? '')
  const [columnName, setColumnName] = useState(firstTable?.columns[0]?.name ?? '')
  const [indexName, setIndexName] = useState('')
  const [expandedTables, setExpandedTables] = useState<string[]>([])
  const [validationError, setValidationError] = useState('')
  const selectedTable = schema?.tables.find((table) => table.name === tableName) ?? firstTable

  useEffect(() => {
    if (!tableName && firstTable) setTableName(firstTable.name)
    if (selectedTable && !selectedTable.columns.some((column) => column.name === columnName)) {
      setColumnName(selectedTable.columns[0]?.name ?? '')
    }
  }, [columnName, firstTable, selectedTable, tableName])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const name = indexName.trim()
    if (!/^[A-Za-z_][A-Za-z0-9_]*$/.test(name)) {
      setValidationError('Use a name beginning with a letter or underscore, followed by letters, numbers, or underscores.')
      return
    }
    if (!selectedTable) { setValidationError('Choose a table.'); return }
    if (!columnName) { setValidationError('Choose a column.'); return }
    setValidationError('')
    const created = await onCreateIndex(name, selectedTable.name, columnName)
    if (created !== false) setIndexName('')
  }

  return (
    <section className="workspace-card schema-card" aria-labelledby="schema-panel-title">
      <div className="card-heading compact-heading">
        <div>
          <div className="eyebrow"><Database size={13} /> CATALOG &amp; INDEXES</div>
          <h2 id="schema-panel-title">In-memory schema</h2>
        </div>
        <button className="icon-button schema-refresh" type="button" onClick={() => void onRefresh()} disabled={isLoading} aria-label="Refresh schema">
          <RefreshCw size={15} className={isLoading ? 'spin' : undefined} />
        </button>
      </div>
      {error ? <div className="schema-feedback" role="alert"><p className="schema-error">{error}</p><button className="secondary-button" type="button" onClick={() => void onRefresh()} disabled={isLoading}>Retry</button></div> : null}
      {success ? <p className="schema-success" role="status">{success}</p> : null}
      {isLoading && !schema ? <p className="schema-muted">Loading schema…</p> : null}
      {!isLoading && !error && schema && schema.tables.length === 0 ? <p className="schema-muted">No tables available.</p> : null}
      {schema && schema.tables.length > 0 ? (
        <div className="schema-layout">
          <div className="schema-tables">
            {schema.tables.map((table) => (
              <article className="schema-table" key={table.name}>
                <button className="schema-table-heading schema-toggle" type="button" aria-expanded={expandedTables.includes(table.name)} onClick={() => setExpandedTables((current) => current.includes(table.name) ? current.filter((name) => name !== table.name) : [...current, table.name])}><strong>{expandedTables.includes(table.name) ? '▾' : '▸'} {table.name}</strong><span>{expandedTables.includes(table.name) ? 'Collapse table' : 'Expand table'} · {statistics?.tables?.find((item) => item.name === table.name)?.rowCount ?? '—'} rows · {table.columns.length} columns</span></button>
                {expandedTables.includes(table.name) && <div className="schema-columns">
                  {table.columns.map((column) => <span key={column.name}><code>{column.name}</code><small>{column.type}</small></span>)}
                </div>}
                <div className="schema-indexes">
                  <span className="schema-label">INDEXES</span>
                  {table.indexes.length ? table.indexes.map((index) => <span className="schema-index" key={index.name}>{index.name} <small>({index.column})</small></span>) : <span className="schema-muted">No indexes</span>}
                  {onUseTable && <button className="schema-use-table" type="button" onClick={() => onUseTable(table.name)}>Use table</button>}
                </div>
              </article>
            ))}
          </div>
          <form className="index-form" onSubmit={(event) => void handleSubmit(event)}>
            <span className="schema-label">CREATE INDEX</span>
            <label htmlFor="index-name">Index name</label>
            <input id="index-name" value={indexName} onChange={(event) => { setIndexName(event.target.value); setValidationError('') }} placeholder="idx_expenses_amount" disabled={isCreating || isLoading} aria-invalid={Boolean(validationError)} aria-describedby={validationError ? 'index-form-error' : undefined} />
            <label htmlFor="index-table">Table</label>
            <select id="index-table" value={selectedTable?.name ?? ''} onChange={(event) => { setTableName(event.target.value); setColumnName('') }} disabled={isCreating || isLoading}>
              {schema.tables.map((table) => <option key={table.name} value={table.name}>{table.name}</option>)}
            </select>
            <label htmlFor="index-column">Column</label>
            <select id="index-column" value={columnName} onChange={(event) => setColumnName(event.target.value)} disabled={isCreating || isLoading || !selectedTable}>
              {selectedTable?.columns.map((column) => <option key={column.name} value={column.name}>{column.name} ({column.type})</option>)}
            </select>
            {validationError ? <p id="index-form-error" className="schema-error" role="alert">{validationError}</p> : null}
            <button className="secondary-button" type="submit" disabled={isCreating || isLoading || !indexName.trim() || !selectedTable || !columnName}><Plus size={14} />{isCreating ? 'Creating…' : 'Create index'}</button>
          </form>
        </div>
      ) : null}
    </section>
  )
}
