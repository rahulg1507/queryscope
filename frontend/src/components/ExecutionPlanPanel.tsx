import { CheckCircle2, GitBranch } from 'lucide-react'
import type { ExecutionPlanNode } from '../api/query'
import { ExecutionPlanNode as ExecutionPlanNodeView } from './ExecutionPlanNode'

type ExecutionPlanPanelProps = {
  plan: ExecutionPlanNode | null
  isExecuting: boolean
}

export function ExecutionPlanPanel({ plan, isExecuting }: ExecutionPlanPanelProps) {
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
        <div className="execution-plan-tree" id="execution-plan-title">
          <ExecutionPlanNodeView node={plan} />
        </div>
      )}
    </section>
  )
}
