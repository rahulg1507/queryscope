import { BarChart3, Play, RefreshCw } from 'lucide-react'
import { useState } from 'react'
import { getBenchmarkCatalog, runBenchmark, type BenchmarkCatalog, type BenchmarkResult } from '../api/benchmarks'

const fallbackCatalog: BenchmarkCatalog = {
  scenarios: ['INDEX_EQUALITY', 'INDEX_RANGE_SELECTIVE', 'INDEX_RANGE_BROAD', 'JOIN_SMALL', 'JOIN_MEDIUM', 'JOIN_LARGE', 'OPTIMIZER_SCAN', 'OPTIMIZER_JOIN'],
  datasetSizes: ['SMALL', 'MEDIUM', 'LARGE'],
}

function label(value: string) {
  return value.replaceAll('_', ' ')
}

function numberValue(value: unknown) {
  return typeof value === 'number' ? value.toLocaleString() : String(value ?? '—')
}

export function BenchmarkPanel() {
  const [catalog, setCatalog] = useState<BenchmarkCatalog>(fallbackCatalog)
  const [scenario, setScenario] = useState('INDEX_EQUALITY')
  const [datasetSize, setDatasetSize] = useState('SMALL')
  const [result, setResult] = useState<BenchmarkResult | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState('')

  async function handleRun() {
    setIsLoading(true)
    setError('')
    try {
      const loaded = await getBenchmarkCatalog()
      if (loaded?.scenarios?.length && loaded?.datasetSizes?.length) setCatalog(loaded)
      setResult(await runBenchmark(scenario, datasetSize))
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'The benchmark could not run.')
      setResult(null)
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <section className="workspace-card benchmark-card" aria-labelledby="benchmark-title">
      <div className="card-heading compact-heading">
        <div>
          <div className="eyebrow"><BarChart3 size={13} /> PERFORMANCE LAB</div>
          <h2 id="benchmark-title">Deterministic benchmarks</h2>
        </div>
        {result && <span className={result.resultsEquivalent ? 'parse-success' : 'comparison-warning'}>{result.resultsEquivalent ? 'EQUIVALENT RESULTS' : 'CHECK RESULTS'}</span>}
      </div>
      <div className="benchmark-controls">
        <label htmlFor="benchmark-scenario">Benchmark</label>
        <select id="benchmark-scenario" value={scenario} onChange={(event) => setScenario(event.target.value)}>
          {catalog.scenarios.map((value) => <option key={value} value={value}>{label(value)}</option>)}
        </select>
        <label htmlFor="benchmark-size">Dataset</label>
        <select id="benchmark-size" value={datasetSize} onChange={(event) => setDatasetSize(event.target.value)}>
          {catalog.datasetSizes.map((value) => <option key={value} value={value}>{value}</option>)}
        </select>
        <button className="secondary-button benchmark-run" type="button" onClick={() => void handleRun()} disabled={isLoading} aria-busy={isLoading}>
          {isLoading ? <RefreshCw className="spin" size={14} /> : <Play size={14} />}
          {isLoading ? 'Running…' : error ? 'Run again' : 'Run benchmark'}
        </button>
      </div>
      {error && <p className="benchmark-error" role="alert">{error}</p>}
      {!result && !error && <p className="benchmark-placeholder">Choose a safe predefined scenario to compare deterministic operation counts. Wall-clock timing is intentionally excluded.</p>}
      {result && (
        <div className="benchmark-results">
          <div className="benchmark-summary"><span>{label(result.scenario)}</span><span>{result.datasetSize} · {numberValue(result.rowsInvolved)} rows involved</span></div>
          <div className="benchmark-comparisons">
            {result.comparisons.map((comparison) => (
              <article className="benchmark-result-card" key={comparison.strategy}>
                <strong>{label(comparison.strategy)}</strong>
                <span>Actual rows returned: <b>{numberValue(comparison.actualRowsReturned)}</b></span>
                {Object.entries(comparison.actualMetrics).filter(([key]) => key !== 'table' && key !== 'index' && key !== 'column' && key !== 'predicate').slice(0, 4).map(([key, value]) => (
                  <span key={key}>{label(key)}: <b>{numberValue(value)}</b></span>
                ))}
              </article>
            ))}
          </div>
          {result.optimizer && (
            <div className="benchmark-optimizer">
              <div className="benchmark-summary"><span>Optimizer selected {label(result.optimizer.selectedPlan)}</span><span>Actual rows: {numberValue(result.optimizer.actualRows)}</span></div>
              <div className="benchmark-candidates">
                {result.optimizer.candidates.map((candidate) => (
                  <div className={`benchmark-candidate ${candidate.planType === result.optimizer?.selectedPlan ? 'selected' : ''}`} key={candidate.planType}>
                    <strong>{label(candidate.planType)}</strong>
                    <span>Est. rows {candidate.estimatedRows.toFixed(2)} · Est. cost {candidate.estimatedCost.toFixed(2)}</span>
                  </div>
                ))}
              </div>
              {result.optimizer.estimationError && <p className="benchmark-muted">Estimation error: {result.optimizer.estimationError.absoluteError.toFixed(2)} rows{result.optimizer.estimationError.percentageError == null ? '' : ` (${result.optimizer.estimationError.percentageError.toFixed(1)}%)`}. Estimated cost is an abstract planning unit; actual work is shown above.</p>}
              {result.optimizer.selectionReason && <p className="benchmark-muted">{result.optimizer.selectionReason}</p>}
            </div>
          )}
          <p className="benchmark-explanation">{result.explanation}</p>
        </div>
      )}
    </section>
  )
}
