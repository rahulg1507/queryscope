import { ArrowDown, Database, Filter, ListFilter } from 'lucide-react'
import type { ExecutionPlanNode as ExecutionPlanNodeData } from '../api/query'

type ExecutionPlanNodeProps = {
  node: ExecutionPlanNodeData
  showMetrics?: boolean
}

function labelFor(type: string) {
  return type.replaceAll('_', ' ')
}

function iconFor(type: string) {
  if (type === 'TABLE_SCAN') return Database
  if (type === 'FILTER') return Filter
  return ListFilter
}

function detailValue(value: unknown) {
  return Array.isArray(value) ? value.join(', ') : String(value)
}

export function ExecutionPlanNode({ node, showMetrics = true }: ExecutionPlanNodeProps) {
  const Icon = iconFor(node.type)
  const isJoin = node.type === 'NESTED_LOOP_JOIN' || node.type === 'HASH_JOIN'

  return (
    <div className={`plan-branch ${isJoin ? 'plan-branch-join' : ''}`}>
      <article className={`plan-node plan-node-${node.type.toLowerCase()}`}>
        <div className="plan-node-heading">
          <span className="plan-node-icon"><Icon size={15} /></span>
          <strong>{labelFor(node.type)}</strong>
        </div>
        <div className="plan-details">
          {Object.entries(node.details).map(([key, value]) => (
            <div className="plan-detail" key={key}>
              <span>{key}</span>
              <strong>{detailValue(value)}</strong>
            </div>
          ))}
        </div>
        {showMetrics && <div className="plan-metrics">
          <span>input <strong>{node.inputRows}</strong></span>
          <span>output <strong>{node.outputRows}</strong></span>
        </div>}
      </article>
      {node.children.length > 0 && (
        <div className="plan-children">
          {node.children.map((child, index) => (
            <div className="plan-child" key={`${child.type}-${index}`}>
              <ArrowDown className="plan-arrow" size={18} aria-hidden="true" />
                <ExecutionPlanNode node={child} showMetrics={showMetrics} />
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
