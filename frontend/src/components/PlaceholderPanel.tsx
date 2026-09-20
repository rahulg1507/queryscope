import { GitBranch, Table2 } from 'lucide-react'

type PlaceholderPanelProps = {
  kind: 'results' | 'plan'
}

export function PlaceholderPanel({ kind }: PlaceholderPanelProps) {
  const isResults = kind === 'results'
  const Icon = isResults ? Table2 : GitBranch

  return (
    <section className="workspace-card placeholder-card" aria-labelledby={`${kind}-title`}>
      <div className="card-heading compact-heading">
        <div className="eyebrow"><Icon size={13} /> {isResults ? 'RESULTS' : 'QUERY PLAN'}</div>
        <span className="panel-state">NOT AVAILABLE</span>
      </div>
      <div className="placeholder-content">
        <span className="placeholder-icon"><Icon size={21} /></span>
        <h2 id={`${kind}-title`}>{isResults ? 'Results will appear here' : 'Plan visualization will appear here'}</h2>
        <p>{isResults ? 'Run results are waiting for the database engine.' : 'The visual execution plan will be added with the planner.'}</p>
      </div>
    </section>
  )
}
