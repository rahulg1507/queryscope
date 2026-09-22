import { useEffect, useRef } from 'react'
import type { WorkspaceSettings } from '../hooks/useWorkspaceSettings'

type DialogProps = { onClose: () => void }

function DialogShell({ title, onClose, children }: DialogProps & { title: string; children: React.ReactNode }) {
  const closeRef = useRef<HTMLButtonElement>(null)
  useEffect(() => {
    closeRef.current?.focus()
    function close(event: KeyboardEvent) { if (event.key === 'Escape') onClose() }
    window.addEventListener('keydown', close)
    return () => window.removeEventListener('keydown', close)
  }, [onClose])
  return <div className="dialog-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}><section className="dialog" role="dialog" aria-modal="true" aria-labelledby="dialog-title"><div className="dialog-heading"><h2 id="dialog-title">{title}</h2><button ref={closeRef} className="icon-button" type="button" aria-label="Close dialog" onClick={onClose}>×</button></div>{children}</section></div>
}

export function HelpDialog({ onClose }: DialogProps) {
  return <DialogShell title="QueryScope help" onClose={onClose}><p className="dialog-lead">QueryScope is an educational in-memory database workspace. Load an example, parse it to inspect the AST, or run it to see rows, metrics, and the physical plan.</p><div className="help-grid"><div><strong>Quick start</strong><p>Choose an example or write a supported SELECT query, then select Run query.</p></div><div><strong>Keyboard shortcuts</strong><p><kbd>Ctrl</kbd>/<kbd>⌘</kbd> + <kbd>Enter</kbd> runs the query.</p><p><kbd>Ctrl</kbd>/<kbd>⌘</kbd> + <kbd>Shift</kbd> + <kbd>Enter</kbd> parses the query.</p><p><kbd>Escape</kbd> closes dialogs and menus.</p></div></div></DialogShell>
}

type SettingsDialogProps = DialogProps & { settings: WorkspaceSettings; onChange: (changes: Partial<WorkspaceSettings>) => void; onReset: () => void }
export function SettingsDialog({ onClose, settings, onChange, onReset }: SettingsDialogProps) {
  return <DialogShell title="Workspace settings" onClose={onClose}><div className="settings-form"><label>Density<select value={settings.density} onChange={(event) => onChange({ density: event.target.value as WorkspaceSettings['density'] })}><option value="comfortable">Comfortable</option><option value="compact">Compact</option></select></label><label><input type="checkbox" checked={settings.showOptimizerDetails} onChange={(event) => onChange({ showOptimizerDetails: event.target.checked })} /> Show optimizer details</label><label><input type="checkbox" checked={settings.showExecutionMetrics} onChange={(event) => onChange({ showExecutionMetrics: event.target.checked })} /> Show execution metrics</label><label><input type="checkbox" checked={settings.autoExpandPlan} onChange={(event) => onChange({ autoExpandPlan: event.target.checked })} /> Auto-expand execution plan</label><label><input type="checkbox" checked={settings.restoreLastQuery} onChange={(event) => onChange({ restoreLastQuery: event.target.checked })} /> Restore last query</label><button className="secondary-button" type="button" onClick={onReset}>Reset defaults</button></div></DialogShell>
}
