import type { BackendStatus as BackendStatusValue } from '../api/health'
import { Activity, CircleAlert, LoaderCircle } from 'lucide-react'

type BackendStatusProps = {
  status: BackendStatusValue
}

export function BackendStatus({ status }: BackendStatusProps) {
  const content = {
    checking: { label: 'Checking backend', icon: <LoaderCircle size={14} className="spin" /> },
    connected: { label: 'Backend connected', icon: <Activity size={14} /> },
    unavailable: { label: 'Backend unavailable', icon: <CircleAlert size={14} /> },
  }[status]

  return (
    <div className={`status-pill status-${status}`} role="status" aria-live="polite">
      {content.icon}
      <span>{content.label}</span>
    </div>
  )
}
