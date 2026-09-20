import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import App from './App'

afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})

describe('App', () => {
  it('renders the query workspace and placeholders', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ status: 'ok' }), { status: 200 }),
    )

    render(<App />)

    expect(screen.getByRole('heading', { name: 'Query workspace' })).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: 'SQL query' })).toHaveValue("SELECT *\nFROM users\nWHERE status = 'active';")
    expect(screen.getByRole('heading', { name: 'Results will appear here' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Plan visualization will appear here' })).toBeInTheDocument()
    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('Backend connected'))
  })

  it('shows backend unavailable when the health check fails', async () => {
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new Error('offline'))

    render(<App />)

    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('Backend unavailable'))
  })
})
