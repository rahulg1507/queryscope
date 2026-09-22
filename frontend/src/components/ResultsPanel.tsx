import { CheckCircle2, Table2 } from 'lucide-react'
import type { QueryResult } from '../api/query'

type ResultsPanelProps = {
  result: QueryResult | null
  isExecuting: boolean
  showMetrics?: boolean
}

export function ResultsPanel({ result, isExecuting, showMetrics = true }: ResultsPanelProps) {
  return (
    <section className="workspace-card results-card" aria-labelledby="results-title">
      <div className="card-heading compact-heading">
        <div className="eyebrow"><Table2 size={13} /> RESULTS</div>
        {result && !isExecuting && <span className="parse-success"><CheckCircle2 size={13} /> EXECUTION SUCCESS</span>}
      </div>
      {!result ? (
        <div className="empty-results">
          <h2 id="results-title">Results will appear here</h2>
          <p>Run a supported SELECT query against the demo database.</p>
        </div>
      ) : (
        <div className="results-body">
          {showMetrics && <div className="results-meta">
            <span><strong>{result.rowCount}</strong> rows</span>
            <span>Scanned <strong>{result.metrics.rowsScanned}</strong></span>
            <span>Returned <strong>{result.metrics.rowsReturned}</strong></span>
          </div>}
          <div className="table-scroll">
            <table>
              <caption className="sr-only">Query results</caption>
              <thead><tr>{result.columns.map((column) => <th key={column.name}><span>{column.name}</span><small>{column.type}</small></th>)}</tr></thead>
              <tbody>
                {result.rows.length === 0 ? (
                  <tr><td className="empty-cell" colSpan={result.columns.length}>No matching rows</td></tr>
                ) : result.rows.map((row, rowIndex) => <tr key={rowIndex}>{row.map((value, cellIndex) => <td key={`${rowIndex}-${cellIndex}`}>{String(value)}</td>)}</tr>)}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </section>
  )
}
