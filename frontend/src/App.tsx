import { useCallback, useEffect, useRef, useState } from 'react'
import { checkBackendHealth, type BackendStatus } from './api/health'
import { HelpDialog, SettingsDialog } from './components/AppDialogs'
import { Navigation } from './components/Navigation'
import { useWorkspaceSettings } from './hooks/useWorkspaceSettings'
import { routeFromLocation, navigateTo, type Route } from './routing'
import { BenchmarksPage } from './pages/BenchmarksPage'
import { DocsPage } from './pages/DocsPage'
import { WorkspacePage } from './pages/WorkspacePage'

function App() {
  const [route, setRoute] = useState<Route>(routeFromLocation)
  const [backendStatus, setBackendStatus] = useState<BackendStatus>('checking')
  const [dialog, setDialog] = useState<'help' | 'settings' | null>(null)
  const wakingTimer = useRef<ReturnType<typeof setTimeout> | null>(null)
  const { settings, updateSettings, resetSettings } = useWorkspaceSettings()

  useEffect(() => { const onPopState = () => setRoute(routeFromLocation()); window.addEventListener('popstate', onPopState); return () => window.removeEventListener('popstate', onPopState) }, [])
  const startHealthCheck = useCallback(() => {
    if (wakingTimer.current) clearTimeout(wakingTimer.current)
    setBackendStatus('checking')
    wakingTimer.current = setTimeout(() => setBackendStatus((current) => current === 'checking' ? 'waking' : current), 1200)
    void checkBackendHealth().then((healthy) => {
      if (wakingTimer.current) clearTimeout(wakingTimer.current)
      setBackendStatus(healthy ? 'connected' : 'unavailable')
    })
  }, [])
  useEffect(() => () => { if (wakingTimer.current) clearTimeout(wakingTimer.current) }, [])
  useEffect(() => { startHealthCheck() }, [startHealthCheck])
  function go(nextRoute: Route) { navigateTo(nextRoute) }

  return <div className={`app-shell density-${settings.density}`}><Navigation route={route} backendStatus={backendStatus} onRetryBackend={startHealthCheck} onNavigate={go} onHelp={() => setDialog('help')} onSettings={() => setDialog('settings')} />{route === '/workspace' ? <WorkspacePage settings={settings} /> : route === '/docs' ? <DocsPage onNavigate={go} /> : <BenchmarksPage />}<footer className="app-footer"><span>QUERYSCOPE <span className="muted-separator">/</span> ENGINEERING PREVIEW</span><span>Built for understanding databases from the inside out.</span></footer>{dialog === 'help' && <HelpDialog onClose={() => setDialog(null)} />}{dialog === 'settings' && <SettingsDialog settings={settings} onChange={updateSettings} onReset={resetSettings} onClose={() => setDialog(null)} />}</div>
}

export default App
