import { useEffect, useState, type FormEvent } from 'react'
import { Database, Plus, RefreshCw } from 'lucide-react'
import type { SchemaResponse } from '../api/query'

type SchemaIndexPanelProps = {
  schema: SchemaResponse | null
  isLoading: boolean
  error: string
  isCreating: boolean
  onRefresh: () => void | Promise<void>
  onCreateIndex: (name: string, table: string, column: string) => void | Promise<void>
}

export function SchemaIndexPanel({ schema, isLoading, error, isCreating, onRefresh, onCreateIndex }: SchemaIndexPanelProps) {
  const firstTable = schema?.tables[0]
  const [tableName, setTableName] = useState(firstTable?.name ?? '')
  const [columnName, setColumnName] = useState(firstTable?.columns[0]?.name ?? '')
  const [indexName, setIndexName] = useState('')
  const selectedTable = schema?.tables.find((table) => table.name === tableName) ?? firstTable

  useEffect(() => {
    if (!tableName && firstTable) setTableName(firstTable.name)
    if (selectedTable && !selectedTable.columns.some((column) => column.name === columnName)) {
      setColumnName(selectedTable.columns[0]?.name ?? '')
    }
  }, [columnName, firstTable, selectedTable, tableName])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!indexName.trim() || !selectedTable || !columnName) return
    await onCreateIndex(indexName.trim(), selectedTable.name, columnName)
    setIndexName('')
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
      {error ? <p className="schema-error" role="alert">{error}</p> : null}
      {isLoading && !schema ? <p className="schema-muted">Loading tables…</p> : null}
      {schema ? (
        <div className="schema-layout">
          <div className="schema-tables">
            {schema.tables.map((table) => (
              <article className="schema-table" key={table.name}>
                <div className="schema-table-heading"><strong>{table.name}</strong><span>{table.columns.length} columns</span></div>
                <div className="schema-columns">
                  {table.columns.map((column) => <span key={column.name}><code>{column.name}</code><small>{column.type}</small></span>)}
                </div>
                <div className="schema-indexes">
                  <span className="schema-label">INDEXES</span>
                  {table.indexes.length ? table.indexes.map((index) => <span className="schema-index" key={index.name}>{index.name} <small>({index.column})</small></span>) : <span className="schema-muted">No indexes</span>}
                </div>
              </article>
            ))}
          </div>
          <form className="index-form" onSubmit={(event) => void handleSubmit(event)}>
            <span className="schema-label">CREATE INDEX</span>
            <label htmlFor="index-name">Index name</label>
            <input id="index-name" value={indexName} onChange={(event) => setIndexName(event.target.value)} placeholder="idx_expenses_amount" disabled={isCreating} />
            <label htmlFor="index-table">Table</label>
            <select id="index-table" value={selectedTable?.name ?? ''} onChange={(event) => { setTableName(event.target.value); setColumnName('') }} disabled={isCreating}>
              {schema.tables.map((table) => <option key={table.name} value={table.name}>{table.name}</option>)}
            </select>
            <label htmlFor="index-column">Column</label>
            <select id="index-column" value={columnName} onChange={(event) => setColumnName(event.target.value)} disabled={isCreating || !selectedTable}>
              {selectedTable?.columns.map((column) => <option key={column.name} value={column.name}>{column.name} ({column.type})</option>)}
            </select>
            <button className="secondary-button" type="submit" disabled={isCreating || !indexName.trim() || !selectedTable || !columnName}><Plus size={14} />{isCreating ? 'Creating…' : 'Create index'}</button>
          </form>
        </div>
      ) : null}
    </section>
  )
}
