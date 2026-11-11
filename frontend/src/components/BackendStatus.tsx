import type { BackendStatus as BackendStatusValue } from '../api/health'
import { Activity, CircleAlert, LoaderCircle } from 'lucide-react'

type BackendStatusProps = {
  status: BackendStatusValue
  onRetry: () => void
}

export function BackendStatus({ status, onRetry }: BackendStatusProps) {
  const content = {
    checking: { label: 'Checking backend', icon: <LoaderCircle size={14} className="spin" /> },
    waking: { label: 'Backend waking up', icon: <LoaderCircle size={14} className="spin" /> },
    connected: { label: 'Backend connected', icon: <Activity size={14} /> },
    unavailable: { label: 'Backend unavailable', icon: <CircleAlert size={14} /> },
  }[status]

  return (
    <div className="status-stack" role="status" aria-live="polite">
      <div className={`status-pill status-${status}`}>
        {content.icon}
        <span>{content.label}</span>
        {status === 'unavailable' && <button className="status-retry" type="button" onClick={onRetry}>Retry</button>}
      </div>
      {status === 'waking' && <span className="status-detail">First request after inactivity may take around a minute.</span>}
    </div>
  )
}
