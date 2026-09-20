import { Database, ScanSearch } from 'lucide-react'

export function BrandMark() {
  return (
    <div className="brand-mark" aria-label="QueryScope home">
      <span className="brand-icon" aria-hidden="true">
        <Database size={19} strokeWidth={2.2} />
        <ScanSearch className="brand-search" size={12} strokeWidth={2.8} />
      </span>
      <span className="brand-copy">
        <strong>QueryScope</strong>
        <span>ENGINEERING WORKSPACE</span>
      </span>
    </div>
  )
}
