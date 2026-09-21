import { useEffect, useState } from 'react'
import { ArrowUpRight, BookOpen, CircleHelp, Layers3, Settings2 } from 'lucide-react'
import { checkBackendHealth, type BackendStatus } from './api/health'
import { createIndex, executeQuery, getSchema, parseQuery, ParseApiError, type JoinStrategy, type ParsedQuery, type QueryResult, type ScanStrategy, type SchemaResponse } from './api/query'
import { BackendStatus as BackendStatusIndicator } from './components/BackendStatus'
import { BrandMark } from './components/BrandMark'
import { ExecutionErrorPanel } from './components/ExecutionErrorPanel'
import { ParseErrorPanel } from './components/ParseErrorPanel'
import { ParsedQueryPanel } from './components/ParsedQueryPanel'
import { ExecutionPlanPanel } from './components/ExecutionPlanPanel'
import { PlaceholderPanel } from './components/PlaceholderPanel'
import { QueryEditor } from './components/QueryEditor'
import { ResultsPanel } from './components/ResultsPanel'
import { SchemaIndexPanel } from './components/SchemaIndexPanel'
import { StrategyComparisonPanel, type StrategyComparison } from './components/StrategyComparisonPanel'

const starterQuery = 'SELECT name, age\nFROM users\nWHERE age > 18;'
type ParseState = 'idle' | 'parsing' | 'success' | 'parser-error' | 'backend-unavailable'
type ExecuteState = 'idle' | 'executing' | 'success' | 'execution-error' | 'backend-unavailable'
type ComparisonState = 'idle' | 'comparing' | 'error' | 'success'

function canonicalRows(result: QueryResult): string[] {
  return result.rows.map((row) => JSON.stringify(row)).sort()
}

function resultsAreEquivalent(left: QueryResult, right: QueryResult): boolean {
  return JSON.stringify(left.columns) === JSON.stringify(right.columns)
    && JSON.stringify(canonicalRows(left)) === JSON.stringify(canonicalRows(right))
}

function App() {
  const [backendStatus, setBackendStatus] = useState<BackendStatus>('checking')
  const [query, setQuery] = useState(starterQuery)
  const [parseState, setParseState] = useState<ParseState>('idle')
  const [parsedQuery, setParsedQuery] = useState<ParsedQuery | null>(null)
  const [parseError, setParseError] = useState('')
  const [executeState, setExecuteState] = useState<ExecuteState>('idle')
  const [queryResult, setQueryResult] = useState<QueryResult | null>(null)
  const [executeError, setExecuteError] = useState('')
  const [joinStrategy, setJoinStrategy] = useState<JoinStrategy>('NESTED_LOOP')
  const [scanStrategy, setScanStrategy] = useState<ScanStrategy>('TABLE')
  const [schema, setSchema] = useState<SchemaResponse | null>(null)
  const [schemaLoading, setSchemaLoading] = useState(true)
  const [schemaError, setSchemaError] = useState('')
  const [creatingIndex, setCreatingIndex] = useState(false)
  const [comparisonState, setComparisonState] = useState<ComparisonState>('idle')
  const [comparison, setComparison] = useState<StrategyComparison | null>(null)
  const [comparisonError, setComparisonError] = useState('')

  useEffect(() => {
    const controller = new AbortController()
    checkBackendHealth(controller.signal).then((isHealthy) => {
      setBackendStatus(isHealthy ? 'connected' : 'unavailable')
    })
    return () => controller.abort()
  }, [])

  async function loadSchema() {
    setSchemaLoading(true)
    setSchemaError('')
    try {
      setSchema(await getSchema())
    } catch (error) {
      setSchemaError(error instanceof Error ? error.message : 'The schema could not be loaded.')
    } finally {
      setSchemaLoading(false)
    }
  }

  useEffect(() => {
    void loadSchema()
  }, [])

  async function handleCreateIndex(name: string, table: string, column: string) {
    setCreatingIndex(true)
    setSchemaError('')
    try {
      setSchema(await createIndex(name, table, column))
    } catch (error) {
      setSchemaError(error instanceof Error ? error.message : 'The index could not be created.')
    } finally {
      setCreatingIndex(false)
    }
  }

  async function handleParse() {
    setParseState('parsing')
    setParseError('')
    setExecuteState('idle')
    setQueryResult(null)
    setComparison(null)
    setComparisonError('')
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
    setComparison(null)
    setComparisonError('')
    let parsed = false
    try {
      const ast = await parseQuery(query)
      parsed = true
      setParsedQuery(ast)
      setParseState('success')
      setQueryResult(await executeQuery(query, joinStrategy, scanStrategy))
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

  async function handleCompare() {
    setComparisonState('comparing')
    setComparison(null)
    setComparisonError('')
    try {
      const ast = await parseQuery(query)
      setParsedQuery(ast)
      setParseState('success')
      const [nestedLoop, hash] = await Promise.all([
        executeQuery(query, 'NESTED_LOOP'),
        executeQuery(query, 'HASH'),
      ])
      setComparison({ nestedLoop, hash, equivalent: resultsAreEquivalent(nestedLoop, hash) })
      setComparisonState('success')
    } catch (error) {
      setComparisonError(error instanceof Error ? error.message : 'The strategies could not be compared.')
      setComparisonState('error')
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
          <div className="version-badge"><span className="live-dot" /> MILESTONE 8 / 0.8</div>
        </div>

        <div className="notice-banner" role="note">
          <span className="notice-mark">i</span>
          <span>Queries run against an in-memory demo database. Results include the actual execution plan; data resets whenever the backend restarts.</span>
          <a href="#roadmap">View roadmap <ArrowUpRight size={14} /></a>
        </div>

        <div className="workspace-grid">
          <SchemaIndexPanel
            schema={schema}
            isLoading={schemaLoading}
            error={schemaError}
            isCreating={creatingIndex}
            onRefresh={loadSchema}
            onCreateIndex={handleCreateIndex}
          />
          <QueryEditor
            query={query}
            onQueryChange={setQuery}
            onParse={handleParse}
            onRun={handleRun}
            onExampleSelect={setQuery}
            joinStrategy={joinStrategy}
            onJoinStrategyChange={setJoinStrategy}
            scanStrategy={scanStrategy}
            onScanStrategyChange={setScanStrategy}
            onCompare={handleCompare}
            isParsing={parseState === 'parsing'}
            isExecuting={executeState === 'executing'}
            isComparing={comparisonState === 'comparing'}
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
            <ExecutionPlanPanel plan={queryResult?.executionPlan ?? null} isExecuting={executeState === 'executing'} />
            <StrategyComparisonPanel comparison={comparison} error={comparisonError} isComparing={comparisonState === 'comparing'} />
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
