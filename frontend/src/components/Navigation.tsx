import { BookOpen, BarChart3, CircleHelp, Layers3, Menu, Map, Settings2, X } from 'lucide-react'
import { useEffect, useState } from 'react'
import { BrandMark } from './BrandMark'
import { BackendStatus as BackendStatusIndicator } from './BackendStatus'
import type { BackendStatus } from '../api/health'
import type { Route } from '../routing'

type NavigationProps = { route: Route; backendStatus: BackendStatus; onNavigate: (route: Route) => void; onHelp: () => void; onSettings: () => void }

const items: Array<{ route: Route; label: string; icon: typeof Layers3 }> = [
  { route: '/workspace', label: 'Workspace', icon: Layers3 },
  { route: '/docs', label: 'Docs', icon: BookOpen },
  { route: '/benchmarks', label: 'Benchmarks', icon: BarChart3 },
  { route: '/roadmap', label: 'Roadmap', icon: Map },
]

export function Navigation({ route, backendStatus, onNavigate, onHelp, onSettings }: NavigationProps) {
  const [menuOpen, setMenuOpen] = useState(false)
  useEffect(() => { setMenuOpen(false) }, [route])
  useEffect(() => {
    function close(event: KeyboardEvent) { if (event.key === 'Escape') setMenuOpen(false) }
    window.addEventListener('keydown', close)
    return () => window.removeEventListener('keydown', close)
  }, [])
  return (
    <header className="topbar">
      <BrandMark />
      <button className="mobile-menu-button icon-button" type="button" aria-label={menuOpen ? 'Close navigation menu' : 'Open navigation menu'} aria-expanded={menuOpen} onClick={() => setMenuOpen((open) => !open)}>
        {menuOpen ? <X size={19} /> : <Menu size={19} />}
      </button>
      <nav className={`top-nav ${menuOpen ? 'mobile-open' : ''}`} aria-label="Main navigation">
        {items.map(({ route: itemRoute, label, icon: Icon }) => <a key={itemRoute} className={`nav-link ${route === itemRoute ? 'active' : ''}`} href={itemRoute} aria-current={route === itemRoute ? 'page' : undefined} onClick={(event) => { event.preventDefault(); onNavigate(itemRoute) }}><Icon size={15} /> {label}</a>)}
        <button className="mobile-nav-action" type="button" onClick={onHelp}><CircleHelp size={15} /> Help</button>
        <button className="mobile-nav-action" type="button" onClick={onSettings}><Settings2 size={15} /> Settings</button>
      </nav>
      <div className="topbar-actions">
        <BackendStatusIndicator status={backendStatus} />
        <button className="icon-button desktop-action" type="button" aria-label="Open help" onClick={onHelp}><CircleHelp size={18} /></button>
        <button className="icon-button desktop-action" type="button" aria-label="Open workspace settings" onClick={onSettings}><Settings2 size={18} /></button>
      </div>
    </header>
  )
}
