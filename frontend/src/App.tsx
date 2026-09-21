import { useEffect, useState } from 'react'
import { ArrowUpRight, BookOpen, CircleHelp, Layers3, Settings2 } from 'lucide-react'
import { checkBackendHealth, type BackendStatus } from './api/health'
import { executeQuery, parseQuery, ParseApiError, type ParsedQuery, type QueryResult } from './api/query'
import { BackendStatus as BackendStatusIndicator } from './components/BackendStatus'
import { BrandMark } from './components/BrandMark'
import { ExecutionErrorPanel } from './components/ExecutionErrorPanel'
import { ParseErrorPanel } from './components/ParseErrorPanel'
import { ParsedQueryPanel } from './components/ParsedQueryPanel'
import { PlaceholderPanel } from './components/PlaceholderPanel'
import { QueryEditor } from './components/QueryEditor'
import { ResultsPanel } from './components/ResultsPanel'

const starterQuery = 'SELECT name, age\nFROM users\nWHERE age > 18;'
type ParseState = 'idle' | 'parsing' | 'success' | 'parser-error' | 'backend-unavailable'
type ExecuteState = 'idle' | 'executing' | 'success' | 'execution-error' | 'backend-unavailable'

function App() {
  const [backendStatus, setBackendStatus] = useState<BackendStatus>('checking')
  const [query, setQuery] = useState(starterQuery)
  const [parseState, setParseState] = useState<ParseState>('idle')
  const [parsedQuery, setParsedQuery] = useState<ParsedQuery | null>(null)
  const [parseError, setParseError] = useState('')
  const [executeState, setExecuteState] = useState<ExecuteState>('idle')
  const [queryResult, setQueryResult] = useState<QueryResult | null>(null)
  const [executeError, setExecuteError] = useState('')

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
    setExecuteState('idle')
    setQueryResult(null)
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

  async function handleRun() {
    setExecuteState('executing')
    setExecuteError('')
    setQueryResult(null)
    let parsed = false
    try {
      const ast = await parseQuery(query)
      parsed = true
      setParsedQuery(ast)
      setParseState('success')
      setQueryResult(await executeQuery(query))
      setExecuteState('success')
    } catch (error) {
      if (!parsed) {
        setParsedQuery(null)
        setParseError(error instanceof Error ? error.message : 'The query could not be parsed.')
        setParseState(error instanceof ParseApiError && error.kind === 'backend' ? 'backend-unavailable' : 'parser-error')
        setExecuteState('idle')
      } else {
        setExecuteError(error instanceof Error ? error.message : 'The query could not execute.')
        setExecuteState(error instanceof ParseApiError && error.kind === 'backend' ? 'backend-unavailable' : 'execution-error')
      }
    }
  }

  const isBusy = parseState === 'parsing' || executeState === 'executing'

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
          <div className="version-badge"><span className="live-dot" /> MILESTONE 3 / 0.3</div>
        </div>

        <div className="notice-banner" role="note">
          <span className="notice-mark">i</span>
          <span>Queries run against a small demo database held in memory. Data resets whenever the backend restarts.</span>
          <a href="#roadmap">View roadmap <ArrowUpRight size={14} /></a>
        </div>

        <div className="workspace-grid">
          <QueryEditor
            query={query}
            onQueryChange={setQuery}
            onParse={handleParse}
            onRun={handleRun}
            onExampleSelect={setQuery}
            isParsing={parseState === 'parsing'}
            isExecuting={executeState === 'executing'}
          />
          <div className="bottom-panels">
            {parseState === 'success' && parsedQuery ? (
              <ParsedQueryPanel ast={parsedQuery} />
            ) : parseState === 'parser-error' || parseState === 'backend-unavailable' ? (
              <ParseErrorPanel message={parseError} backendUnavailable={parseState === 'backend-unavailable'} />
            ) : (
              <PlaceholderPanel kind="results" />
            )}
            {executeState === 'execution-error' || executeState === 'backend-unavailable' ? (
              <ExecutionErrorPanel message={executeError} />
            ) : (
              <ResultsPanel result={queryResult} isExecuting={executeState === 'executing'} />
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
