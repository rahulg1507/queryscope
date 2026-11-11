import type { OptimizerCandidate } from './query'
import { requestJson } from './client'

export type BenchmarkCatalog = { scenarios: string[]; datasetSizes: string[] }
export type BenchmarkComparison = {
  strategy: string
  planType: string
  actualRowsReturned: number
  actualMetrics: Record<string, unknown>
}
export type BenchmarkEstimationError = {
  estimatedRows: number
  actualRows: number
  absoluteError: number
  percentageError: number | null
}
export type BenchmarkOptimizer = {
  selectedPlan: string
  estimatedRows: number | null
  estimatedCost: number | null
  selectionReason: string | null
  candidates: OptimizerCandidate[]
  estimationError: BenchmarkEstimationError | null
  actualRows: number
  actualMetrics: Record<string, unknown>
}
export type BenchmarkResult = {
  scenario: string
  datasetSize: string
  rowsInvolved: number
  strategiesCompared: string[]
  resultsEquivalent: boolean
  comparisons: BenchmarkComparison[]
  optimizer: BenchmarkOptimizer | null
  explanation: string
}

async function benchmarkRequest(path: string, init?: RequestInit): Promise<unknown> {
  return requestJson(path, init)
}

export async function getBenchmarkCatalog(): Promise<BenchmarkCatalog> {
  return await benchmarkRequest('/api/benchmarks/scenarios') as BenchmarkCatalog
}

export async function runBenchmark(scenario: string, datasetSize: string): Promise<BenchmarkResult> {
  return await benchmarkRequest('/api/benchmarks/run', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ scenario, datasetSize }),
  }) as BenchmarkResult
}
