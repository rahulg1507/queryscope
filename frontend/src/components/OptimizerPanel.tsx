import { CheckCircle2, GitBranch } from 'lucide-react'
import type { OptimizationInfo } from '../api/query'

type OptimizerPanelProps = {
  optimization: OptimizationInfo | null | undefined
}

export function OptimizerPanel({ optimization }: OptimizerPanelProps) {
  return (
    <section className="workspace-card optimizer-card" aria-labelledby="optimizer-panel-title">
      <div className="card-heading compact-heading">
        <div>
          <div className="eyebrow"><GitBranch size={13} /> OPTIMIZER</div>
          <h2 id="optimizer-panel-title">Why this plan?</h2>
        </div>
        {optimization ? <span className="panel-state">{optimization.mode} MODE</span> : <span className="panel-state">WAITING</span>}
      </div>
      {optimization ? (
        <div className="optimizer-trace">
          {optimization.candidates?.length ? (
            <div className="optimizer-candidates" aria-label="Cost-based plan candidates">
              <div className="optimizer-summary">
                <strong>{optimization.selectedPlan.replaceAll('_', ' ')}</strong>
                {optimization.estimatedRows != null && <span>{optimization.estimatedRows.toFixed(2)} estimated rows</span>}
                {optimization.estimatedCost != null && <span>{optimization.estimatedCost.toFixed(2)} cost units</span>}
              </div>
              <p className="estimate-label">ESTIMATED PLANNING VALUES · ACTUAL ROWS SCANNED/RETURNED ARE SHOWN IN RESULTS</p>
              <div className="candidate-grid">
                {optimization.candidates.map((candidate, index) => (
                  <div className={`candidate-card ${candidate.planType === optimization.selectedPlan ? 'selected' : ''}`} key={`${candidate.planType}-${index}`}>
                    <strong>{candidate.planType.replaceAll('_', ' ')}</strong>
                    <span>{candidate.estimatedRows.toFixed(2)} rows · {candidate.estimatedCost.toFixed(2)} cost</span>
                  </div>
                ))}
              </div>
              {optimization.selectionReason && <p className="selection-reason">{optimization.selectionReason}</p>}
            </div>
          ) : null}
          {optimization.rulesApplied.length ? optimization.rulesApplied.map((trace, index) => (
            <article className="optimizer-rule" key={`${trace.rule}-${index}`}>
              <CheckCircle2 size={15} />
              <div><strong>{trace.rule.replaceAll('_', ' ')}</strong><span>→ {trace.decision.replaceAll('_', ' ')}</span><p>{trace.reason}</p></div>
            </article>
          )) : <p className="schema-muted">No rewrite rules were needed for this plan.</p>}
        </div>
      ) : (
        <div className="optimizer-placeholder"><p>Run a query to see the deterministic rule-based optimizer trace.</p></div>
      )}
    </section>
  )
}
