import { GitCompare } from 'lucide-react'
import type { ExecutionPlanNode, QueryResult } from '../api/query'

export type StrategyComparison = {
  nestedLoop: QueryResult
  hash: QueryResult
  equivalent: boolean
}

type StrategyComparisonPanelProps = {
  comparison: StrategyComparison | null
  error: string
  isComparing: boolean
}

function findJoin(node: ExecutionPlanNode): ExecutionPlanNode | null {
  if (node.type === 'NESTED_LOOP_JOIN' || node.type === 'HASH_JOIN') return node
  for (const child of node.children) {
    const join = findJoin(child)
    if (join) return join
  }
  return null
}

function metric(result: QueryResult, key: string): string {
  const join = findJoin(result.executionPlan)
  const value = join?.details[key]
  return value === undefined ? '—' : String(value)
}

export function StrategyComparisonPanel({ comparison, error, isComparing }: StrategyComparisonPanelProps) {
  return (
    <section className="workspace-card comparison-card" aria-labelledby="comparison-title">
      <div className="card-heading compact-heading">
        <div className="eyebrow"><GitCompare size={13} /> STRATEGY COMPARISON</div>
        {comparison && <span className={comparison.equivalent ? 'parse-success' : 'comparison-warning'}>{comparison.equivalent ? 'EQUIVALENT RESULTS' : 'RESULTS DIFFER'}</span>}
      </div>
      {isComparing ? (
        <div className="comparison-placeholder"><h2 id="comparison-title">Comparing join strategies…</h2></div>
      ) : error ? (
        <div className="comparison-placeholder comparison-error" role="alert"><h2 id="comparison-title">Comparison failed</h2><p>{error}</p></div>
      ) : !comparison ? (
        <div className="comparison-placeholder"><h2 id="comparison-title">Compare both join strategies</h2><p>Run Compare strategies on a JOIN query to inspect algorithm-specific metrics.</p></div>
      ) : (
        <div className="comparison-table-wrap" id="comparison-title">
          <table className="comparison-table">
            <thead><tr><th>Metric</th><th>Nested Loop</th><th>Hash Join</th></tr></thead>
            <tbody>
              <tr><td>Output rows</td><td>{comparison.nestedLoop.rowCount}</td><td>{comparison.hash.rowCount}</td></tr>
              <tr><td>Build side</td><td>—</td><td>{metric(comparison.hash, 'buildSide')}</td></tr>
              <tr><td>Build rows</td><td>—</td><td>{metric(comparison.hash, 'buildRows')}</td></tr>
              <tr><td>Rows inserted</td><td>—</td><td>{metric(comparison.hash, 'rowsInserted')}</td></tr>
              <tr><td>Probe rows</td><td>—</td><td>{metric(comparison.hash, 'probeRows')}</td></tr>
              <tr><td>Comparisons / lookups</td><td>{metric(comparison.nestedLoop, 'comparisons')}</td><td>{metric(comparison.hash, 'hashLookups')}</td></tr>
              <tr><td>Matches</td><td>{metric(comparison.nestedLoop, 'matches')}</td><td>{metric(comparison.hash, 'matches')}</td></tr>
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
