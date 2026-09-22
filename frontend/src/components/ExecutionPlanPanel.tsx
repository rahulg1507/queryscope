import { CheckCircle2, GitBranch } from 'lucide-react'
import type { ExecutionPlanNode } from '../api/query'
import { ExecutionPlanNode as ExecutionPlanNodeView } from './ExecutionPlanNode'

type ExecutionPlanPanelProps = {
  plan: ExecutionPlanNode | null
  isExecuting: boolean
  showMetrics?: boolean
  autoExpand?: boolean
}

export function ExecutionPlanPanel({ plan, isExecuting, showMetrics = true, autoExpand = true }: ExecutionPlanPanelProps) {
  return (
    <section className="workspace-card plan-card" aria-labelledby="execution-plan-title">
      <div className="card-heading compact-heading">
        <div className="eyebrow"><GitBranch size={13} /> QUERY PLAN</div>
        {plan && !isExecuting && <span className="parse-success"><CheckCircle2 size={13} /> PLAN GENERATED</span>}
      </div>
      {!plan ? (
        <div className="placeholder-content">
          <span className="placeholder-icon"><GitBranch size={21} /></span>
          <h2 id="execution-plan-title">Plan visualization will appear here</h2>
          <p>Run a supported SELECT query to inspect its execution pipeline.</p>
        </div>
      ) : (
        autoExpand ? <div className="execution-plan-tree" id="execution-plan-title"><ExecutionPlanNodeView node={plan} showMetrics={showMetrics} /></div> : <details className="plan-collapsed" id="execution-plan-title"><summary>Show execution plan</summary><div className="execution-plan-tree"><ExecutionPlanNodeView node={plan} showMetrics={showMetrics} /></div></details>
      )}
    </section>
  )
}
