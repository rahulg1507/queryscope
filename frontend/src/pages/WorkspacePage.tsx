import { useEffect, useRef, useState } from 'react'
import { createIndex, executeQuery, getSchema, parseQuery, ParseApiError, type ExecutionMode, type JoinStrategy, type ParsedQuery, type QueryResult, type ScanStrategy, type SchemaResponse, type StatisticsResponse } from '../api/query'
import { ExecutionErrorPanel } from '../components/ExecutionErrorPanel'
import { ParseErrorPanel } from '../components/ParseErrorPanel'
import { ParsedQueryPanel } from '../components/ParsedQueryPanel'
import { ExecutionPlanPanel } from '../components/ExecutionPlanPanel'
import { PlaceholderPanel } from '../components/PlaceholderPanel'
import { QueryEditor } from '../components/QueryEditor'
import { ResultsPanel } from '../components/ResultsPanel'
import { SchemaIndexPanel } from '../components/SchemaIndexPanel'
import { OptimizerPanel } from '../components/OptimizerPanel'
import { StrategyComparisonPanel, type StrategyComparison } from '../components/StrategyComparisonPanel'
import { QueryHistoryPanel } from '../components/QueryHistoryPanel'
import { useQueryHistory } from '../hooks/useQueryHistory'
import type { WorkspaceSettings } from '../hooks/useWorkspaceSettings'

const starterQuery = 'SELECT name, age\nFROM users\nWHERE age > 18;'
type ParseState = 'idle' | 'parsing' | 'success' | 'parser-error' | 'backend-unavailable'
type ExecuteState = 'idle' | 'executing' | 'success' | 'execution-error' | 'backend-unavailable'
type ComparisonState = 'idle' | 'comparing' | 'error' | 'success'

function canonicalRows(result: QueryResult): string[] { return result.rows.map((row) => JSON.stringify(row)).sort() }
function resultsAreEquivalent(left: QueryResult, right: QueryResult): boolean { return JSON.stringify(left.columns) === JSON.stringify(right.columns) && JSON.stringify(canonicalRows(left)) === JSON.stringify(canonicalRows(right)) }
function messageFor(error: unknown, fallback: string) { return error instanceof Error && error.message ? error.message : fallback }

type Props = { settings: WorkspaceSettings }
export function WorkspacePage({ settings }: Props) {
  const [query, setQuery] = useState(starterQuery)
  const [parseState, setParseState] = useState<ParseState>('idle')
  const [parsedQuery, setParsedQuery] = useState<ParsedQuery | null>(null)
  const [parseError, setParseError] = useState('')
  const [executeState, setExecuteState] = useState<ExecuteState>('idle')
  const [queryResult, setQueryResult] = useState<QueryResult | null>(null)
  const [executeError, setExecuteError] = useState('')
  const [joinStrategy, setJoinStrategy] = useState<JoinStrategy>('NESTED_LOOP')
  const [scanStrategy, setScanStrategy] = useState<ScanStrategy>('TABLE')
  const [executionMode, setExecutionMode] = useState<ExecutionMode>('AUTO')
  const [schema, setSchema] = useState<SchemaResponse | null>(null)
  const [statistics, setStatistics] = useState<StatisticsResponse | null>(null)
  const [schemaLoading, setSchemaLoading] = useState(true)
  const [schemaError, setSchemaError] = useState('')
  const [schemaSuccess, setSchemaSuccess] = useState('')
  const [creatingIndex, setCreatingIndex] = useState(false)
  const [queryFeedback, setQueryFeedback] = useState('')
  const [comparisonState, setComparisonState] = useState<ComparisonState>('idle')
  const [comparison, setComparison] = useState<StrategyComparison | null>(null)
  const [comparisonError, setComparisonError] = useState('')
  const { history, addHistory, removeHistory, clearHistory } = useQueryHistory()
  const schemaRequest = useRef(false)

  useEffect(() => { const pending = localStorage.getItem('queryscope.pendingQuery'); if (pending) { setQuery(pending); localStorage.removeItem('queryscope.pendingQuery'); return }; if (settings.restoreLastQuery) { const saved = localStorage.getItem('queryscope.lastQuery'); if (saved) setQuery(saved) } }, [settings.restoreLastQuery])
  useEffect(() => { if (settings.restoreLastQuery && query.trim()) localStorage.setItem('queryscope.lastQuery', query) }, [query, settings.restoreLastQuery])

  async function loadSchema(): Promise<SchemaResponse | null> {
    if (schemaRequest.current) return null
    schemaRequest.current = true
    setSchemaLoading(true)
    setSchemaError('')
    setSchemaSuccess('')
    try {
      const loaded = await getSchema()
      setSchema(loaded)
      setSchemaSuccess('Schema refreshed.')
      return loaded
    } catch (error) {
      setSchemaError(`Unable to load schema. ${messageFor(error, 'Check that the QueryScope API is running and try again.')}`)
      return null
    } finally {
      schemaRequest.current = false
      setSchemaLoading(false)
    }
  }
  useEffect(() => { void loadSchema() }, [])
  async function handleCreateIndex(name: string, table: string, column: string) {
    if (creatingIndex) return false
    setCreatingIndex(true)
    setSchemaError('')
    setSchemaSuccess('')
    try {
      const createdSchema = await createIndex(name, table, column)
      setSchema(createdSchema)
      const refreshedSchema = await loadSchema()
      if (refreshedSchema) {
        const mergedTables = refreshedSchema.tables.map((item) => {
          const createdTable = createdSchema.tables.find((created) => created.name === item.name)
          return createdTable && createdTable.indexes.length > item.indexes.length ? { ...item, indexes: createdTable.indexes } : item
        })
        setSchema({ ...refreshedSchema, tables: mergedTables })
      }
      setSchemaSuccess(`Index ${name} created.`)
      return true
    } catch (error) {
      setSchemaError(`Unable to create index. ${messageFor(error, 'Check the index name, table, and column, then try again.')}`)
      return false
    } finally {
      setCreatingIndex(false)
    }
  }

  function clearQueryOutput() {
    setParseState('idle')
    setParsedQuery(null)
    setParseError('')
    setExecuteState('idle')
    setQueryResult(null)
    setExecuteError('')
    setComparisonState('idle')
    setComparison(null)
    setComparisonError('')
  }

  function updateQuery(nextQuery: string) {
    setQuery(nextQuery)
    clearQueryOutput()
    setQueryFeedback('')
  }

  function clearEditor() {
    updateQuery('')
    setQueryFeedback('Editor cleared.')
  }

  function useTable(name: string) {
    updateQuery(`SELECT * FROM ${name};`)
    setQueryFeedback('Query loaded into workspace.')
    window.requestAnimationFrame(() => {
      const editor = document.getElementById('sql-editor')
      editor?.focus()
      editor?.scrollIntoView?.({ behavior: 'smooth', block: 'center' })
    })
  }

  async function handleParse(sql = query) { setParseState('parsing'); setParseError(''); setExecuteState('idle'); setQueryResult(null); setComparisonState('idle'); setComparison(null); setComparisonError(''); try { setParsedQuery(await parseQuery(sql)); setParseState('success'); addHistory(sql, 'parsed') } catch (error) { setParsedQuery(null); setParseError(messageFor(error, 'The query could not be parsed.')); setParseState(error instanceof ParseApiError && error.kind === 'backend' ? 'backend-unavailable' : 'parser-error'); addHistory(sql, 'parse error') } }
  async function handleRun(sql = query) { setExecuteState('executing'); setExecuteError(''); setQueryResult(null); setComparisonState('idle'); setComparison(null); setComparisonError(''); let parsed = false; try { setParsedQuery(await parseQuery(sql)); parsed = true; setParseState('success'); setQueryResult(await executeQuery(sql, joinStrategy, scanStrategy, executionMode)); setExecuteState('success'); addHistory(sql, 'success') } catch (error) { if (!parsed) { setParsedQuery(null); setParseError(messageFor(error, 'The query could not be parsed.')); setParseState(error instanceof ParseApiError && error.kind === 'backend' ? 'backend-unavailable' : 'parser-error'); setExecuteState('idle'); addHistory(sql, 'parse error') } else { setExecuteError(messageFor(error, 'The query could not execute.')); setExecuteState(error instanceof ParseApiError && error.kind === 'backend' ? 'backend-unavailable' : 'execution-error'); addHistory(sql, 'execution error') } } }
  async function handleCompare() { setComparisonState('comparing'); setComparison(null); setComparisonError(''); try { setParsedQuery(await parseQuery(query)); setParseState('success'); const [nestedLoop, hash] = await Promise.all([executeQuery(query, 'NESTED_LOOP', 'TABLE', 'MANUAL'), executeQuery(query, 'HASH', 'TABLE', 'MANUAL')]); setComparison({ nestedLoop, hash, equivalent: resultsAreEquivalent(nestedLoop, hash) }); setComparisonState('success') } catch (error) { setComparisonError(error instanceof Error ? error.message : 'The strategies could not be compared.'); setComparisonState('error') } }
  useEffect(() => { function shortcut(event: KeyboardEvent) { if (!(event.ctrlKey || event.metaKey) || event.key.toLowerCase() !== 'enter') return; event.preventDefault(); if (event.shiftKey) void handleParse(); else void handleRun() }; window.addEventListener('keydown', shortcut); return () => window.removeEventListener('keydown', shortcut) })

  const parseBusy = parseState === 'parsing'; const executeBusy = executeState === 'executing'
  return <main id="workspace" className="main-content"><div className="page-intro"><div><p className="section-kicker">EXPERIMENTAL RELATIONAL ENGINE</p><h1>Query workspace</h1><p className="intro-copy">Explore how QueryScope will parse, plan, and execute SQL.</p></div><div className="version-badge"><span className="live-dot" /> DEVELOPER PREVIEW</div></div><div className="notice-banner" role="note"><span className="notice-mark">i</span><span>Queries run against an in-memory demo database. Results include the actual execution plan; data resets whenever the backend restarts.</span></div><div className="workspace-grid"><SchemaIndexPanel schema={schema} statistics={statistics} isLoading={schemaLoading} error={schemaError} success={schemaSuccess} isCreating={creatingIndex} onRefresh={loadSchema} onCreateIndex={handleCreateIndex} onUseTable={useTable} /><QueryEditor query={query} onQueryChange={updateQuery} onParse={() => handleParse()} onRun={() => handleRun()} onClear={clearEditor} onExampleSelect={updateQuery} joinStrategy={joinStrategy} onJoinStrategyChange={setJoinStrategy} scanStrategy={scanStrategy} onScanStrategyChange={setScanStrategy} executionMode={executionMode} onExecutionModeChange={setExecutionMode} onCompare={handleCompare} isParsing={parseBusy} isExecuting={executeBusy} isComparing={comparisonState === 'comparing'} /><div className="workspace-feedback" role="status" aria-live="polite">{queryFeedback}</div><QueryHistoryPanel history={history} onLoad={updateQuery} onRunAgain={(sql) => { updateQuery(sql); void handleRun(sql) }} onRemove={removeHistory} onClear={clearHistory} /><div className="bottom-panels">{parseState === 'success' && parsedQuery ? <ParsedQueryPanel ast={parsedQuery} /> : parseState === 'parser-error' || parseState === 'backend-unavailable' ? <ParseErrorPanel message={parseError} backendUnavailable={parseState === 'backend-unavailable'} /> : <PlaceholderPanel kind="results" />}{executeState === 'execution-error' || executeState === 'backend-unavailable' ? <ExecutionErrorPanel message={executeError} /> : <ResultsPanel result={queryResult} isExecuting={executeBusy} showMetrics={settings.showExecutionMetrics} />}<ExecutionPlanPanel plan={queryResult?.executionPlan ?? null} isExecuting={executeBusy} showMetrics={settings.showExecutionMetrics} autoExpand={settings.autoExpandPlan} />{settings.showOptimizerDetails && <OptimizerPanel optimization={queryResult?.optimization} />}<StrategyComparisonPanel comparison={comparison} error={comparisonError} isComparing={comparisonState === 'comparing'} /></div></div></main>
}
