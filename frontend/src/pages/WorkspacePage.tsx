import { useEffect, useState } from 'react'
import { ArrowUpRight } from 'lucide-react'
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
import type { Route } from '../routing'

const starterQuery = 'SELECT name, age\nFROM users\nWHERE age > 18;'
type ParseState = 'idle' | 'parsing' | 'success' | 'parser-error' | 'backend-unavailable'
type ExecuteState = 'idle' | 'executing' | 'success' | 'execution-error' | 'backend-unavailable'
type ComparisonState = 'idle' | 'comparing' | 'error' | 'success'

function canonicalRows(result: QueryResult): string[] { return result.rows.map((row) => JSON.stringify(row)).sort() }
function resultsAreEquivalent(left: QueryResult, right: QueryResult): boolean { return JSON.stringify(left.columns) === JSON.stringify(right.columns) && JSON.stringify(canonicalRows(left)) === JSON.stringify(canonicalRows(right)) }

type Props = { settings: WorkspaceSettings; onNavigate: (route: Route) => void }
export function WorkspacePage({ settings, onNavigate }: Props) {
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
  const [creatingIndex, setCreatingIndex] = useState(false)
  const [comparisonState, setComparisonState] = useState<ComparisonState>('idle')
  const [comparison, setComparison] = useState<StrategyComparison | null>(null)
  const [comparisonError, setComparisonError] = useState('')
  const { history, addHistory, removeHistory, clearHistory } = useQueryHistory()

  useEffect(() => { const pending = localStorage.getItem('queryscope.pendingQuery'); if (pending) { setQuery(pending); localStorage.removeItem('queryscope.pendingQuery'); return }; if (settings.restoreLastQuery) { const saved = localStorage.getItem('queryscope.lastQuery'); if (saved) setQuery(saved) } }, [settings.restoreLastQuery])
  useEffect(() => { if (settings.restoreLastQuery && query.trim()) localStorage.setItem('queryscope.lastQuery', query) }, [query, settings.restoreLastQuery])

  async function loadSchema() { setSchemaLoading(true); setSchemaError(''); try { setSchema(await getSchema()) } catch (error) { setSchemaError(error instanceof Error ? error.message : 'The schema could not be loaded.') } finally { setSchemaLoading(false) } }
  useEffect(() => { void loadSchema() }, [])
  async function handleCreateIndex(name: string, table: string, column: string) { setCreatingIndex(true); setSchemaError(''); try { setSchema(await createIndex(name, table, column)) } catch (error) { setSchemaError(error instanceof Error ? error.message : 'The index could not be created.') } finally { setCreatingIndex(false) } }

  async function handleParse(sql = query) { setParseState('parsing'); setParseError(''); setExecuteState('idle'); setQueryResult(null); setComparison(null); setComparisonError(''); try { setParsedQuery(await parseQuery(sql)); setParseState('success'); addHistory(sql, 'parsed') } catch (error) { setParsedQuery(null); setParseError(error instanceof Error ? error.message : 'The query could not be parsed.'); setParseState(error instanceof ParseApiError && error.kind === 'backend' ? 'backend-unavailable' : 'parser-error'); addHistory(sql, 'parse error') } }
  async function handleRun(sql = query) { setExecuteState('executing'); setExecuteError(''); setQueryResult(null); setComparison(null); setComparisonError(''); let parsed = false; try { setParsedQuery(await parseQuery(sql)); parsed = true; setParseState('success'); setQueryResult(await executeQuery(sql, joinStrategy, scanStrategy, executionMode)); setExecuteState('success'); addHistory(sql, 'success') } catch (error) { if (!parsed) { setParsedQuery(null); setParseError(error instanceof Error ? error.message : 'The query could not be parsed.'); setParseState(error instanceof ParseApiError && error.kind === 'backend' ? 'backend-unavailable' : 'parser-error'); setExecuteState('idle'); addHistory(sql, 'parse error') } else { setExecuteError(error instanceof Error ? error.message : 'The query could not execute.'); setExecuteState(error instanceof ParseApiError && error.kind === 'backend' ? 'backend-unavailable' : 'execution-error'); addHistory(sql, 'execution error') } } }
  async function handleCompare() { setComparisonState('comparing'); setComparison(null); setComparisonError(''); try { setParsedQuery(await parseQuery(query)); setParseState('success'); const [nestedLoop, hash] = await Promise.all([executeQuery(query, 'NESTED_LOOP', 'TABLE', 'MANUAL'), executeQuery(query, 'HASH', 'TABLE', 'MANUAL')]); setComparison({ nestedLoop, hash, equivalent: resultsAreEquivalent(nestedLoop, hash) }); setComparisonState('success') } catch (error) { setComparisonError(error instanceof Error ? error.message : 'The strategies could not be compared.'); setComparisonState('error') } }
  useEffect(() => { function shortcut(event: KeyboardEvent) { if (!(event.ctrlKey || event.metaKey) || event.key.toLowerCase() !== 'enter') return; event.preventDefault(); if (event.shiftKey) void handleParse(); else void handleRun() }; window.addEventListener('keydown', shortcut); return () => window.removeEventListener('keydown', shortcut) })

  const parseBusy = parseState === 'parsing'; const executeBusy = executeState === 'executing'
  return <main id="workspace" className="main-content"><div className="page-intro"><div><p className="section-kicker">EXPERIMENTAL RELATIONAL ENGINE</p><h1>Query workspace</h1><p className="intro-copy">Explore how QueryScope will parse, plan, and execute SQL.</p></div><div className="version-badge"><span className="live-dot" /> MILESTONE 12 / 1.0</div></div><div className="notice-banner" role="note"><span className="notice-mark">i</span><span>Queries run against an in-memory demo database. Results include the actual execution plan; data resets whenever the backend restarts.</span><a href="/roadmap" onClick={(event) => { event.preventDefault(); onNavigate('/roadmap') }}>View roadmap <ArrowUpRight size={14} /></a></div><div className="workspace-grid"><SchemaIndexPanel schema={schema} statistics={statistics} isLoading={schemaLoading} error={schemaError} isCreating={creatingIndex} onRefresh={loadSchema} onCreateIndex={handleCreateIndex} onUseTable={(name) => setQuery(`SELECT * FROM ${name};`)} /><QueryEditor query={query} onQueryChange={setQuery} onParse={() => handleParse()} onRun={() => handleRun()} onClear={() => setQuery('')} onExampleSelect={setQuery} joinStrategy={joinStrategy} onJoinStrategyChange={setJoinStrategy} scanStrategy={scanStrategy} onScanStrategyChange={setScanStrategy} executionMode={executionMode} onExecutionModeChange={setExecutionMode} onCompare={handleCompare} isParsing={parseBusy} isExecuting={executeBusy} isComparing={comparisonState === 'comparing'} /><QueryHistoryPanel history={history} onLoad={setQuery} onRunAgain={(sql) => { setQuery(sql); void handleRun(sql) }} onRemove={removeHistory} onClear={clearHistory} /><div className="bottom-panels">{parseState === 'success' && parsedQuery ? <ParsedQueryPanel ast={parsedQuery} /> : parseState === 'parser-error' || parseState === 'backend-unavailable' ? <ParseErrorPanel message={parseError} backendUnavailable={parseState === 'backend-unavailable'} /> : <PlaceholderPanel kind="results" />}{executeState === 'execution-error' || executeState === 'backend-unavailable' ? <ExecutionErrorPanel message={executeError} /> : <ResultsPanel result={queryResult} isExecuting={executeBusy} showMetrics={settings.showExecutionMetrics} />}<ExecutionPlanPanel plan={queryResult?.executionPlan ?? null} isExecuting={executeBusy} showMetrics={settings.showExecutionMetrics} autoExpand={settings.autoExpandPlan} />{settings.showOptimizerDetails && <OptimizerPanel optimization={queryResult?.optimization} />}<StrategyComparisonPanel comparison={comparison} error={comparisonError} isComparing={comparisonState === 'comparing'} /></div></div></main>
}
