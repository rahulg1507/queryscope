import { Code2, GitCompare, Play } from 'lucide-react'
import type { ExecutionMode, JoinStrategy, ScanStrategy } from '../api/query'

type QueryEditorProps = {
  query: string
  onQueryChange: (query: string) => void
  onParse: () => void | Promise<void>
  onRun: () => void | Promise<void>
  onExampleSelect: (query: string) => void
  joinStrategy: JoinStrategy
  onJoinStrategyChange: (strategy: JoinStrategy) => void
  scanStrategy: ScanStrategy
  onScanStrategyChange: (strategy: ScanStrategy) => void
  executionMode: ExecutionMode
  onExecutionModeChange: (mode: ExecutionMode) => void
  onCompare: () => void | Promise<void>
  isParsing: boolean
  isExecuting: boolean
  isComparing: boolean
}

const examples = [
  'SELECT * FROM users;',
  'SELECT name, age FROM users;',
  'SELECT name FROM users WHERE age > 18;',
  'SELECT name FROM users WHERE active = true;',
  'SELECT users.name, expenses.amount\nFROM users\nJOIN expenses\nON users.id = expenses.user_id;',
  'SELECT COUNT(*) FROM users;',
  'SELECT user_id, SUM(amount)\nFROM expenses\nGROUP BY user_id;',
  'SELECT user_id, AVG(amount)\nFROM expenses\nGROUP BY user_id;',
]

export function QueryEditor({ query, onQueryChange, onParse, onRun, onExampleSelect, joinStrategy, onJoinStrategyChange, scanStrategy, onScanStrategyChange, executionMode, onExecutionModeChange, onCompare, isParsing, isExecuting, isComparing }: QueryEditorProps) {
  const isBusy = isParsing || isExecuting || isComparing
  const hasJoin = /\bjoin\b/i.test(query)
  const hasWhere = /\bwhere\b/i.test(query)

  return (
    <section className="workspace-card editor-card" aria-labelledby="query-editor-title">
      <div className="card-heading">
        <div>
          <div className="eyebrow"><Code2 size={13} /> QUERY EDITOR</div>
          <h2 id="query-editor-title">Write a query</h2>
        </div>
        <span className="coming-soon">ENGINE IN DEVELOPMENT</span>
      </div>
      <label className="sr-only" htmlFor="sql-editor">SQL query</label>
      <textarea
        id="sql-editor"
        className="sql-editor"
        value={query}
        onChange={(event) => onQueryChange(event.target.value)}
        spellCheck={false}
        aria-describedby="editor-note"
      />
      <div className="editor-footer">
        <p id="editor-note">Parse the statement to inspect its AST, or run it against the demo in-memory data.</p>
        <div className="editor-actions">
          <button className="secondary-button" type="button" onClick={() => void onParse()} disabled={isBusy}>
            {isParsing ? 'Parsing…' : 'Parse'}
          </button>
          <button className="run-button" type="button" onClick={() => void onRun()} disabled={isBusy}>
            <Play size={15} fill="currentColor" />
            {isExecuting ? 'Executing…' : 'Run query'}
          </button>
        </div>
      </div>
      <div className="strategy-controls">
        <label htmlFor="execution-mode">Execution mode</label>
        <select
          id="execution-mode"
          value={executionMode}
          onChange={(event) => onExecutionModeChange(event.target.value as ExecutionMode)}
          disabled={isBusy}
        >
          <option value="AUTO">Auto</option>
          <option value="MANUAL">Manual</option>
        </select>
        <span>{executionMode === 'AUTO' ? 'QueryScope selects eligible physical strategies using deterministic rules.' : 'Manual mode honors the selected scan and join strategies.'}</span>
      </div>
      <div className="strategy-controls">
        <label htmlFor="join-strategy">Join strategy</label>
        <select
          id="join-strategy"
          value={joinStrategy}
          onChange={(event) => onJoinStrategyChange(event.target.value as JoinStrategy)}
          disabled={!hasJoin || executionMode !== 'MANUAL' || isBusy}
        >
          <option value="NESTED_LOOP">Nested Loop</option>
          <option value="HASH">Hash Join</option>
        </select>
        <span>{hasJoin ? 'Selected manually; no optimizer is applied.' : 'Select a JOIN query to choose a strategy.'}</span>
        <button className="secondary-button compare-button" type="button" onClick={() => void onCompare()} disabled={!hasJoin || executionMode !== 'MANUAL' || isBusy}>
          <GitCompare size={14} />
          {isComparing ? 'Comparing…' : 'Compare strategies'}
        </button>
      </div>
      <div className="strategy-controls scan-controls">
        <label htmlFor="scan-strategy">Scan strategy</label>
        <select
          id="scan-strategy"
          value={scanStrategy}
          onChange={(event) => onScanStrategyChange(event.target.value as ScanStrategy)}
          disabled={!hasWhere || executionMode !== 'MANUAL' || isBusy}
        >
          <option value="TABLE">Table Scan</option>
          <option value="INDEX">Index Scan</option>
        </select>
        <span>{executionMode === 'AUTO' ? 'Auto mode chooses an existing matching index when eligible.' : hasWhere ? 'Index scans require a matching single-column index.' : 'Add a WHERE predicate to choose an index scan.'}</span>
      </div>
      <div className="examples-row" aria-label="Example queries">
        <span>EXAMPLES</span>
        {examples.map((example) => <button key={example} type="button" onClick={() => onExampleSelect(example)} disabled={isBusy}>{example}</button>)}
      </div>
    </section>
  )
}
