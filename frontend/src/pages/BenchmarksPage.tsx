import { BenchmarkPanel } from '../components/BenchmarkPanel'

export function BenchmarksPage() { return <main className="main-content benchmark-page"><div className="page-intro"><div><p className="section-kicker">DETERMINISTIC ANALYSIS</p><h1>Benchmarks</h1><p className="intro-copy">Compare operation counts and optimizer estimates on isolated datasets.</p></div></div><BenchmarkPanel /></main> }
