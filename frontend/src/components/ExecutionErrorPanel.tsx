import { AlertCircle } from 'lucide-react'

type ExecutionErrorPanelProps = {
  message: string
  onRetry?: () => void
}

export function ExecutionErrorPanel({ message, onRetry }: ExecutionErrorPanelProps) {
  return (
    <section className="workspace-card parse-error-card" role="alert" aria-labelledby="execution-error-title">
      <div className="card-heading compact-heading">
        <div className="eyebrow"><AlertCircle size={13} /> QUERY ERROR</div>
        <span className="panel-state">EXECUTION FAILED</span>
      </div>
      <div className="parse-error-content">
        <h2 id="execution-error-title">Query could not execute</h2>
        <p>{message}</p>
        {onRetry && <button className="secondary-button" type="button" onClick={onRetry}>Retry</button>}
      </div>
    </section>
  )
}
