import { useEffect, useRef, type ReactNode } from 'react'
import type { WorkspaceSettings } from '../hooks/useWorkspaceSettings'

type DialogProps = { onClose: () => void }

function DialogShell({ title, onClose, children }: DialogProps & { title: string; children: ReactNode }) {
  const closeRef = useRef<HTMLButtonElement>(null)
  const dialogRef = useRef<HTMLElement>(null)
  const previousFocus = useRef<HTMLElement | null>(document.activeElement instanceof HTMLElement ? document.activeElement : null)
  const closeHandler = useRef(onClose)
  closeHandler.current = onClose
  useEffect(() => {
    closeRef.current?.focus()
    function close(event: KeyboardEvent) {
      if (event.key === 'Escape') { event.preventDefault(); closeHandler.current() }
      if (event.key !== 'Tab') return
      const focusable = dialogRef.current?.querySelectorAll<HTMLElement>('button, input, select, textarea, a[href], [tabindex]:not([tabindex="-1"])')
      if (!focusable?.length) return
      const first = focusable[0]
      const last = focusable[focusable.length - 1]
      if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus() }
      else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus() }
    }
    window.addEventListener('keydown', close)
    return () => { window.removeEventListener('keydown', close); previousFocus.current?.focus() }
  }, [])
  return <div className="dialog-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) closeHandler.current() }}><section ref={dialogRef} className="dialog" role="dialog" aria-modal="true" aria-labelledby="dialog-title"><div className="dialog-heading"><h2 id="dialog-title">{title}</h2><button ref={closeRef} className="icon-button" type="button" aria-label="Close dialog" onClick={onClose}>×</button></div>{children}</section></div>
}

export function HelpDialog({ onClose }: DialogProps) {
  return <DialogShell title="QueryScope help" onClose={onClose}><div className="help-copy"><strong>WHAT IS QUERYSCOPE?</strong><p>QueryScope is an experimental relational database engine that lets you write SQL, inspect execution plans, and compare different ways of executing a query.</p></div><div className="help-grid"><div><strong>QUICK START</strong><ol><li>Choose or write SQL.</li><li>Parse it to inspect the AST.</li><li>Run it to execute the query.</li><li>Inspect the execution plan.</li><li>Inspect optimizer decisions.</li><li>Use Benchmarks to compare strategies.</li></ol></div><div><strong>KEY SHORTCUT</strong><p><kbd>Ctrl</kbd>/<kbd>⌘</kbd> + <kbd>Enter</kbd> → Run Query</p><p><kbd>Ctrl</kbd>/<kbd>⌘</kbd> + <kbd>Shift</kbd> + <kbd>Enter</kbd> → Parse Query</p><p><kbd>Escape</kbd> closes dialogs and menus.</p></div></div></DialogShell>
}

type SettingsDialogProps = DialogProps & { settings: WorkspaceSettings; onChange: (changes: Partial<WorkspaceSettings>) => void; onReset: () => void }
export function SettingsDialog({ onClose, settings, onChange, onReset }: SettingsDialogProps) {
  return <DialogShell title="Workspace settings" onClose={onClose}><div className="settings-form"><label><span>Density</span><select aria-label="Density" value={settings.density} onChange={(event) => onChange({ density: event.target.value as WorkspaceSettings['density'] })}><option value="comfortable">Comfortable</option><option value="compact">Compact</option></select></label><label><input type="checkbox" checked={settings.showOptimizerDetails} onChange={(event) => onChange({ showOptimizerDetails: event.target.checked })} /> Show optimizer details</label><label><input type="checkbox" checked={settings.showExecutionMetrics} onChange={(event) => onChange({ showExecutionMetrics: event.target.checked })} /> Show execution metrics</label><label><input type="checkbox" checked={settings.autoExpandPlan} onChange={(event) => onChange({ autoExpandPlan: event.target.checked })} /> Auto-expand execution plan</label><label><input type="checkbox" checked={settings.restoreLastQuery} onChange={(event) => onChange({ restoreLastQuery: event.target.checked })} /> Restore last query</label><button className="secondary-button" type="button" onClick={onReset}>Reset to defaults</button></div></DialogShell>
}
