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

function healthyFetch(parseResponse: Response | Error = jsonResponse({
  type: 'SELECT',
  columns: [{ type: 'COLUMN', name: 'name' }],
  from: { type: 'TABLE', name: 'users' },
  where: { type: 'COMPARISON', operator: 'GREATER_THAN', left: { type: 'COLUMN', name: 'age' }, right: { type: 'NUMBER', value: 18 } },
})) {
  return vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
    if (input.toString() === '/api/health') return jsonResponse({ status: 'ok' })
    if (parseResponse instanceof Error) throw parseResponse
    return parseResponse
  })
}

describe('App', () => {
  it('renders the editor and reports a successful parsed AST', async () => {
    const fetchMock = healthyFetch()
    const user = userEvent.setup()
    render(<App />)

    expect(screen.getByRole('heading', { name: 'Query workspace' })).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: 'SQL query' })).toHaveValue("SELECT *\nFROM users\nWHERE status = 'active';")
    expect(screen.getByRole('button', { name: 'Run query' })).toBeEnabled()

    await user.click(screen.getByRole('button', { name: 'Run query' }))

    await waitFor(() => expect(screen.getByText('PARSE SUCCESS')).toBeInTheDocument())
    expect(screen.getByText('users')).toBeInTheDocument()
    expect(screen.getByText('GREATER_THAN')).toBeInTheDocument()
    const parseCall = fetchMock.mock.calls.find(([input]) => input.toString() === '/api/query/parse')
    expect(parseCall?.[1]).toEqual(expect.objectContaining({ method: 'POST' }))
  })

  it('shows a parser error returned by the API', async () => {
    const fetchMock = healthyFetch(jsonResponse({ error: 'Expected FROM after SELECT list at position 14' }, 400))
    const user = userEvent.setup()
    render(<App />)

    await user.click(screen.getByRole('button', { name: 'Run query' }))

    await waitFor(() => expect(screen.getByRole('alert')).toBeInTheDocument())
    expect(screen.getByText('Expected FROM after SELECT list at position 14')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalled()
  })

  it('shows backend unavailable when parsing cannot reach the API', async () => {
    healthyFetch(new Error('offline'))
    const user = userEvent.setup()
    render(<App />)

    await user.click(screen.getByRole('button', { name: 'Run query' }))

    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Backend unavailable'))
  })

  it('disables Run while parsing and restores it afterward', async () => {
    let resolveParse!: (response: Response) => void
    const pendingResponse = new Promise<Response>((resolve) => { resolveParse = resolve })
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      if (input.toString() === '/api/health') return jsonResponse({ status: 'ok' })
      return pendingResponse
    })
    const user = userEvent.setup()
    render(<App />)

    const button = screen.getByRole('button', { name: 'Run query' })
    await user.click(button)
    expect(screen.getByRole('button', { name: 'Parsing…' })).toBeDisabled()

    resolveParse(jsonResponse({
      type: 'SELECT',
      columns: [{ type: 'WILDCARD' }],
      from: { type: 'TABLE', name: 'users' },
      where: null,
    }))
    await waitFor(() => expect(screen.getByRole('button', { name: 'Run query' })).toBeEnabled())
  })
})
