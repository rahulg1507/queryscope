import { Code2, Play } from 'lucide-react'

type QueryEditorProps = {
  query: string
  onQueryChange: (query: string) => void
  onRun: () => void | Promise<void>
  isParsing: boolean
}

export function QueryEditor({ query, onQueryChange, onRun, isParsing }: QueryEditorProps) {
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
        <p id="editor-note">SQL execution will be available as the QueryScope engine evolves.</p>
        <button className="run-button" type="button" onClick={() => void onRun()} disabled={isParsing}>
          <Play size={15} fill="currentColor" />
          {isParsing ? 'Parsing…' : 'Run query'}
        </button>
      </div>
    </section>
  )
}
