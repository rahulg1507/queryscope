import { Code2, GitCompare, Play } from 'lucide-react'
import type { JoinStrategy } from '../api/query'

type QueryEditorProps = {
  query: string
  onQueryChange: (query: string) => void
  onParse: () => void | Promise<void>
  onRun: () => void | Promise<void>
  onExampleSelect: (query: string) => void
  joinStrategy: JoinStrategy
  onJoinStrategyChange: (strategy: JoinStrategy) => void
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

export function QueryEditor({ query, onQueryChange, onParse, onRun, onExampleSelect, joinStrategy, onJoinStrategyChange, onCompare, isParsing, isExecuting, isComparing }: QueryEditorProps) {
  const isBusy = isParsing || isExecuting || isComparing
  const hasJoin = /\bjoin\b/i.test(query)

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
        <label htmlFor="join-strategy">Join strategy</label>
        <select
          id="join-strategy"
          value={joinStrategy}
          onChange={(event) => onJoinStrategyChange(event.target.value as JoinStrategy)}
          disabled={!hasJoin || isBusy}
        >
          <option value="NESTED_LOOP">Nested Loop</option>
          <option value="HASH">Hash Join</option>
        </select>
        <span>{hasJoin ? 'Selected manually; no optimizer is applied.' : 'Select a JOIN query to choose a strategy.'}</span>
        <button className="secondary-button compare-button" type="button" onClick={() => void onCompare()} disabled={!hasJoin || isBusy}>
          <GitCompare size={14} />
          {isComparing ? 'Comparing…' : 'Compare strategies'}
        </button>
      </div>
      <div className="examples-row" aria-label="Example queries">
        <span>EXAMPLES</span>
        {examples.map((example) => <button key={example} type="button" onClick={() => onExampleSelect(example)} disabled={isBusy}>{example}</button>)}
      </div>
    </section>
  )
}
