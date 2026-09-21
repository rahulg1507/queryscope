import { AlertCircle } from 'lucide-react'

type ParseErrorPanelProps = {
  message: string
  backendUnavailable?: boolean
}

export function ParseErrorPanel({ message, backendUnavailable = false }: ParseErrorPanelProps) {
  return (
    <section className="workspace-card parse-error-card" role="alert" aria-labelledby="parse-error-title">
      <div className="card-heading compact-heading">
        <div className="eyebrow"><AlertCircle size={13} /> {backendUnavailable ? 'BACKEND CONNECTION' : 'PARSER ERROR'}</div>
        <span className="panel-state">REQUEST FAILED</span>
      </div>
      <div className="parse-error-content">
        <h2 id="parse-error-title">{backendUnavailable ? 'Could not reach the backend' : 'Query was not parsed'}</h2>
        <p>{message}</p>
      </div>
    </section>
  )
}
