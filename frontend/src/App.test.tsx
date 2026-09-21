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
    expect(fetchMock.mock.calls.some(([input]) => input.toString() === '/api/query/execute')).toBe(true)
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
  })
})
