import { afterEach, describe, expect, it, vi } from 'vitest'
import userEvent from '@testing-library/user-event'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import App from './App'

afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})

function jsonResponse(payload: unknown, status = 200) {
  return new Response(JSON.stringify(payload), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

const parsedAst = {
  type: 'SELECT',
  columns: [
    { type: 'COLUMN', name: 'name' },
    { type: 'COLUMN', name: 'age' },
  ],
  from: { type: 'TABLE', name: 'users' },
  where: {
    type: 'COMPARISON',
    operator: 'GREATER_THAN',
    left: { type: 'COLUMN', name: 'age' },
    right: { type: 'NUMBER', value: 18 },
  },
}

const queryResult = {
  columns: [
    { name: 'name', type: 'STRING' },
    { name: 'age', type: 'INTEGER' },
  ],
  rows: [['Rahul', 19], ['Aayan', 21], ['Maya', 25]],
  rowCount: 3,
  metrics: { rowsScanned: 4, rowsReturned: 3 },
  executionPlan: {
    type: 'PROJECTION',
    details: { columns: ['name', 'age'] },
    inputRows: 3,
    outputRows: 3,
    children: [{
      type: 'FILTER',
      details: { condition: 'age > 18' },
      inputRows: 4,
      outputRows: 3,
      children: [{
        type: 'TABLE_SCAN',
        details: { table: 'users' },
        inputRows: 4,
        outputRows: 4,
        children: [],
      }],
    }],
  },
}

const joinResult = {
  columns: [{ name: 'users.name', type: 'STRING' }, { name: 'expenses.amount', type: 'INTEGER' }],
  rows: [['Rahul', 90], ['Aayan', 300], ['Rahul', 40], ['John', 60]],
  rowCount: 4,
  metrics: { rowsScanned: 4, rowsReturned: 4 },
  executionPlan: {
    type: 'PROJECTION',
    details: { columns: ['users.name', 'expenses.amount'] },
    inputRows: 4,
    outputRows: 4,
    children: [{
      type: 'NESTED_LOOP_JOIN',
      details: { condition: 'users.id = expenses.user_id', leftRows: 4, rightRows: 4, comparisons: 16, matches: 4 },
      inputRows: 8,
      outputRows: 4,
      children: [
        { type: 'TABLE_SCAN', details: { table: 'users' }, inputRows: 4, outputRows: 4, children: [] },
        { type: 'TABLE_SCAN', details: { table: 'expenses' }, inputRows: 4, outputRows: 4, children: [] },
      ],
    }],
  },
}

const hashJoinResult = {
  ...joinResult,
  executionPlan: {
    ...joinResult.executionPlan,
    children: [{
      ...joinResult.executionPlan.children[0],
      type: 'HASH_JOIN',
      details: {
        condition: 'users.id = expenses.user_id',
        strategy: 'HASH',
        buildSide: 'LEFT',
        buildRows: 4,
        rowsInserted: 4,
        probeRows: 4,
        hashLookups: 4,
        matches: 4,
      },
    }],
  },
}

function healthyFetch(
  parseResponse: Response | Error | Promise<Response> = jsonResponse(parsedAst),
  executeResponse: Response | Error | Promise<Response> = jsonResponse(queryResult),
) {
  return vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
    const path = input.toString()
    if (path === '/api/health') return jsonResponse({ status: 'ok' })
    const response = path.endsWith('/parse') ? parseResponse : executeResponse
    if (response instanceof Error) throw response
    return response
  })
}

describe('App', () => {
  it('renders the editor and reports a successful parsed AST', async () => {
    const fetchMock = healthyFetch()
    const user = userEvent.setup()
    render(<App />)

    expect(screen.getByRole('heading', { name: 'Query workspace' })).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: 'SQL query' })).toHaveValue('SELECT name, age\nFROM users\nWHERE age > 18;')

    await user.click(screen.getByRole('button', { name: 'Parse' }))

    await waitFor(() => expect(screen.getByText('PARSE SUCCESS')).toBeInTheDocument())
    expect(screen.getByText('users')).toBeInTheDocument()
    expect(screen.getByText('GREATER_THAN')).toBeInTheDocument()
    const parseCall = fetchMock.mock.calls.find(([input]) => input.toString() === '/api/query/parse')
    expect(parseCall?.[1]).toEqual(expect.objectContaining({ method: 'POST' }))
  })

  it('runs the query and renders rows and execution metrics', async () => {
    const fetchMock = healthyFetch()
    const user = userEvent.setup()
    render(<App />)

    await user.click(screen.getByRole('button', { name: 'Run query' }))

    await waitFor(() => expect(screen.getByText('EXECUTION SUCCESS')).toBeInTheDocument())
    expect(screen.getByText('Rahul')).toBeInTheDocument()
    expect(screen.getByText('Scanned').parentElement).toHaveTextContent('4')
    expect(screen.getByText('PROJECTION')).toBeInTheDocument()
    expect(screen.getByText('FILTER')).toBeInTheDocument()
    expect(screen.getByText('TABLE SCAN')).toBeInTheDocument()
    expect(screen.getByText('age > 18')).toBeInTheDocument()
    expect(fetchMock.mock.calls.some(([input]) => input.toString() === '/api/query/execute')).toBe(true)
  })

  it('loads the JOIN example and renders a branching plan', async () => {
    healthyFetch(jsonResponse(parsedAst), jsonResponse(joinResult))
    const user = userEvent.setup()
    render(<App />)

    await user.click(screen.getByRole('button', { name: /SELECT users\.name, expenses\.amount/ }))
    expect(screen.getByRole('textbox', { name: 'SQL query' })).toHaveValue(
      'SELECT users.name, expenses.amount\nFROM users\nJOIN expenses\nON users.id = expenses.user_id;',
    )
    await user.click(screen.getByRole('button', { name: 'Run query' }))

    await waitFor(() => expect(screen.getByText('NESTED LOOP JOIN')).toBeInTheDocument())
    expect(screen.getAllByText('TABLE SCAN')).toHaveLength(2)
    expect(screen.getByText('comparisons')).toBeInTheDocument()
    expect(screen.getByText('16')).toBeInTheDocument()
    expect(screen.getAllByText('Rahul')).toHaveLength(2)
  })

  it('selects hash execution and compares equivalent join results', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const path = input.toString()
      if (path === '/api/health') return jsonResponse({ status: 'ok' })
      if (path.endsWith('/parse')) return jsonResponse(parsedAst)
      const body = JSON.parse(String(init?.body ?? '{}')) as { joinStrategy?: string }
      return jsonResponse(body.joinStrategy === 'HASH' ? hashJoinResult : joinResult)
    })
    const user = userEvent.setup()
    render(<App />)

    await user.click(screen.getByRole('button', { name: /SELECT users\.name, expenses\.amount/ }))
    const strategy = screen.getByLabelText('Join strategy')
    expect(strategy).toBeEnabled()
    await user.selectOptions(strategy, 'HASH')
    await user.click(screen.getByRole('button', { name: 'Run query' }))

    await waitFor(() => expect(screen.getByText('HASH JOIN')).toBeInTheDocument())
    const executeCall = fetchMock.mock.calls.find(([input, init]) => input.toString() === '/api/query/execute' && String(init?.body).includes('"HASH"'))
    expect(executeCall).toBeDefined()

    await user.click(screen.getByRole('button', { name: 'Compare strategies' }))
    await waitFor(() => expect(screen.getByText('EQUIVALENT RESULTS')).toBeInTheDocument())
    expect(screen.getByText('Comparisons / lookups')).toBeInTheDocument()
    expect(screen.getByText('Rows inserted')).toBeInTheDocument()
  })

  it('shows a parser error returned by the API', async () => {
    healthyFetch(jsonResponse({ error: 'Expected FROM after SELECT list at position 14' }, 400))
    const user = userEvent.setup()
    render(<App />)

    await user.click(screen.getByRole('button', { name: 'Parse' }))

    await waitFor(() => expect(screen.getByRole('alert')).toBeInTheDocument())
    expect(screen.getByText('Expected FROM after SELECT list at position 14')).toBeInTheDocument()
  })

  it('shows a query execution error returned by the API', async () => {
    healthyFetch(jsonResponse(parsedAst), jsonResponse({ error: "Unknown table 'missing'." }, 400))
    const user = userEvent.setup()
    render(<App />)

    await user.click(screen.getByRole('button', { name: 'Run query' }))

    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent("Unknown table 'missing'."))
  })

  it('clears a previous plan when a later execution fails', async () => {
    let executeCalls = 0
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const path = input.toString()
      if (path === '/api/health') return jsonResponse({ status: 'ok' })
      if (path.endsWith('/parse')) return jsonResponse(parsedAst)
      executeCalls += 1
      return executeCalls === 1
        ? jsonResponse(queryResult)
        : jsonResponse({ error: 'Unknown table.' }, 400)
    })
    const user = userEvent.setup()
    render(<App />)

    await user.click(screen.getByRole('button', { name: 'Run query' }))
    await waitFor(() => expect(screen.getByText('PLAN GENERATED')).toBeInTheDocument())

    await user.click(screen.getByRole('button', { name: 'Run query' }))
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Unknown table.'))
    expect(screen.queryByText('PLAN GENERATED')).not.toBeInTheDocument()
    expect(screen.getByText('Plan visualization will appear here')).toBeInTheDocument()
  })

  it('shows backend unavailable when parsing cannot reach the API', async () => {
    healthyFetch(new Error('offline'))
    const user = userEvent.setup()
    render(<App />)

    await user.click(screen.getByRole('button', { name: 'Parse' }))

    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Backend unavailable'))
  })

  it('disables Run while executing and restores it afterward', async () => {
    let resolveExecute!: (response: Response) => void
    const pendingExecute = new Promise<Response>((resolve) => { resolveExecute = resolve })
    healthyFetch(jsonResponse(parsedAst), pendingExecute)
    const user = userEvent.setup()
    render(<App />)

    await user.click(screen.getByRole('button', { name: 'Run query' }))
    await waitFor(() => expect(screen.getByRole('button', { name: 'Executing…' })).toBeDisabled())

    resolveExecute(jsonResponse(queryResult))
    await waitFor(() => expect(screen.getByRole('button', { name: 'Run query' })).toBeEnabled())
  })

  it('renders an empty result set clearly', async () => {
    healthyFetch(jsonResponse(parsedAst), jsonResponse({ ...queryResult, rows: [], rowCount: 0, metrics: { rowsScanned: 4, rowsReturned: 0 } }))
    const user = userEvent.setup()
    render(<App />)

    await user.click(screen.getByRole('button', { name: 'Run query' }))

    await waitFor(() => expect(screen.getByText('No matching rows')).toBeInTheDocument())
    expect(screen.getByText((_, element) => element?.textContent === '0 rows')).toBeInTheDocument()
    expect(screen.getByText('PLAN GENERATED')).toBeInTheDocument()
  })
})
