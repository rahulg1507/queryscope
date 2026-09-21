import { useEffect, useState } from 'react'
import { ArrowUpRight, BookOpen, CircleHelp, Layers3, Settings2 } from 'lucide-react'
import { checkBackendHealth, type BackendStatus } from './api/health'
import { parseQuery, ParseApiError, type ParsedQuery } from './api/query'
import { BackendStatus as BackendStatusIndicator } from './components/BackendStatus'
import { BrandMark } from './components/BrandMark'
import { ParseErrorPanel } from './components/ParseErrorPanel'
import { ParsedQueryPanel } from './components/ParsedQueryPanel'
import { PlaceholderPanel } from './components/PlaceholderPanel'
import { QueryEditor } from './components/QueryEditor'

const starterQuery = 'SELECT *\nFROM users\nWHERE status = \'active\';'
type ParseState = 'idle' | 'parsing' | 'success' | 'parser-error' | 'backend-unavailable'

function App() {
  const [backendStatus, setBackendStatus] = useState<BackendStatus>('checking')
  const [query, setQuery] = useState(starterQuery)
  const [parseState, setParseState] = useState<ParseState>('idle')
  const [parsedQuery, setParsedQuery] = useState<ParsedQuery | null>(null)
  const [parseError, setParseError] = useState('')

  useEffect(() => {
    const controller = new AbortController()
    checkBackendHealth(controller.signal).then((isHealthy) => {
      setBackendStatus(isHealthy ? 'connected' : 'unavailable')
    })
    return () => controller.abort()
  }, [])

  async function handleParse() {
    setParseState('parsing')
    setParseError('')
    try {
      const ast = await parseQuery(query)
      setParsedQuery(ast)
      setParseState('success')
    } catch (error) {
      setParsedQuery(null)
      if (error instanceof ParseApiError && error.kind === 'backend') {
        setParseError(error.message)
        setParseState('backend-unavailable')
      } else {
        setParseError(error instanceof Error ? error.message : 'The query could not be parsed.')
        setParseState('parser-error')
      }
    }
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <BrandMark />
        <nav className="top-nav" aria-label="Main navigation">
          <a className="nav-link active" href="#workspace"><Layers3 size={15} /> Workspace</a>
          <a className="nav-link" href="#docs"><BookOpen size={15} /> Docs</a>
        </nav>
        <div className="topbar-actions">
          <BackendStatusIndicator status={backendStatus} />
          <button className="icon-button" type="button" aria-label="Help"><CircleHelp size={18} /></button>
          <button className="icon-button" type="button" aria-label="Settings"><Settings2 size={18} /></button>
        </div>
      </header>

      <main id="workspace" className="main-content">
        <div className="page-intro">
          <div>
            <p className="section-kicker">EXPERIMENTAL RELATIONAL ENGINE</p>
            <h1>Query workspace</h1>
            <p className="intro-copy">Explore how QueryScope will parse, plan, and execute SQL.</p>
          </div>
          <div className="version-badge"><span className="live-dot" /> FOUNDATION / 0.1</div>
        </div>

        <div className="notice-banner" role="note">
          <span className="notice-mark">i</span>
          <span>The database engine is under active development. Query execution and result data are not available yet.</span>
          <a href="#roadmap">View roadmap <ArrowUpRight size={14} /></a>
        </div>

        <div className="workspace-grid">
          <QueryEditor query={query} onQueryChange={setQuery} onRun={handleParse} isParsing={parseState === 'parsing'} />
          <div className="bottom-panels">
            {parseState === 'success' && parsedQuery ? (
              <ParsedQueryPanel ast={parsedQuery} />
            ) : parseState === 'parser-error' || parseState === 'backend-unavailable' ? (
              <ParseErrorPanel message={parseError} backendUnavailable={parseState === 'backend-unavailable'} />
            ) : (
              <PlaceholderPanel kind="results" />
            )}
            <PlaceholderPanel kind="plan" />
          </div>
        </div>
      </main>

      <footer className="app-footer">
        <span>QUERYSCOPE <span className="muted-separator">/</span> ENGINEERING PREVIEW</span>
        <span>Built for understanding databases from the inside out.</span>
      </footer>
    </div>
  )
}

export default App
