import { CheckCircle2, ChevronDown, GitBranch } from 'lucide-react'

type ParsedQueryPanelProps = {
  ast: unknown
}

function AstNode({ label, value }: { label: string; value: unknown }) {
  if (value === null || value === undefined) return null

  if (Array.isArray(value)) {
    return (
      <div className="ast-group">
        <div className="ast-key">{label}</div>
        <div className="ast-children">
          {value.map((item, index) => <AstNode key={`${label}-${index}`} label={`item ${index + 1}`} value={item} />)}
        </div>
      </div>
    )
  }

  if (typeof value === 'object') {
    const objectValue = value as Record<string, unknown>
    const type = typeof objectValue.type === 'string' ? objectValue.type : undefined
    return (
      <details className="ast-node" open>
        <summary><ChevronDown size={13} /> <span>{label}</span>{type && <em>{type}</em>}</summary>
        <div className="ast-children">
          {Object.entries(objectValue)
            .filter(([key]) => key !== 'type')
            .map(([key, child]) => <AstNode key={key} label={key} value={child} />)}
        </div>
      </details>
    )
  }

  return (
    <div className="ast-value-row">
      <span className="ast-key">{label}</span>
      <span className="ast-value">{String(value)}</span>
    </div>
  )
}

export function ParsedQueryPanel({ ast }: ParsedQueryPanelProps) {
  return (
    <section className="workspace-card parsed-card" aria-labelledby="parsed-query-title">
      <div className="card-heading compact-heading">
        <div className="eyebrow"><GitBranch size={13} /> PARSED QUERY</div>
        <span className="parse-success"><CheckCircle2 size={13} /> PARSE SUCCESS</span>
      </div>
      <div className="ast-tree" id="parsed-query-title">
        <AstNode label="SELECT" value={ast} />
      </div>
    </section>
  )
}
