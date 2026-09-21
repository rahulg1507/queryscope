import { afterEach, describe, expect, it, vi } from 'vitest'
import userEvent from '@testing-library/user-event'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import { BenchmarkPanel } from './BenchmarkPanel'

afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})

const catalog = { scenarios: ['INDEX_EQUALITY', 'OPTIMIZER_JOIN'], datasetSizes: ['SMALL', 'MEDIUM'] }
const result = {
  scenario: 'INDEX_EQUALITY',
  datasetSize: 'SMALL',
  rowsInvolved: 1000,
  strategiesCompared: ['TABLE_SCAN', 'INDEX_SCAN'],
  resultsEquivalent: true,
  comparisons: [
    { strategy: 'TABLE_SCAN', planType: 'TABLE_SCAN', actualRowsReturned: 2, actualMetrics: { rowsScanned: 1000, rowsReturned: 2 } },
    { strategy: 'INDEX_SCAN', planType: 'INDEX_SCAN', actualRowsReturned: 2, actualMetrics: { indexLookups: 1, leafEntriesVisited: 2, rowsReturned: 2 } },
  ],
  optimizer: null,
  explanation: 'TableScan inspected 1000 rows; IndexScan visited 2 index entries for the same logical result.',
}

describe('BenchmarkPanel', () => {
  it('loads scenarios, runs a benchmark, and renders operation counts', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      if (input.toString().endsWith('/scenarios')) return new Response(JSON.stringify(catalog), { status: 200 })
      return new Response(JSON.stringify(result), { status: 200 })
    })
    const user = userEvent.setup()
    render(<BenchmarkPanel />)

    expect(screen.getByLabelText('Benchmark')).toHaveValue('INDEX_EQUALITY')
    await user.click(screen.getByRole('button', { name: 'Run benchmark' }))

    await waitFor(() => expect(screen.getByText('EQUIVALENT RESULTS')).toBeInTheDocument())
    expect(screen.getByText('TABLE SCAN')).toBeInTheDocument()
    expect(screen.getByText('1,000')).toBeInTheDocument()
    expect(fetchMock.mock.calls.some(([input]) => input.toString().endsWith('/run'))).toBe(true)
  })

  it('renders benchmark errors without stale results', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      if (input.toString().endsWith('/scenarios')) return new Response(JSON.stringify(catalog), { status: 200 })
      return new Response(JSON.stringify({ error: 'Benchmark unavailable' }), { status: 400 })
    })
    const user = userEvent.setup()
    render(<BenchmarkPanel />)

    await user.click(screen.getByRole('button', { name: 'Run benchmark' }))
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Benchmark unavailable'))
    expect(screen.queryByText('EQUIVALENT RESULTS')).not.toBeInTheDocument()
  })
})
