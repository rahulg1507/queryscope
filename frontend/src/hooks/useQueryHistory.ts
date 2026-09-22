import { useEffect, useState } from 'react'

export const historyStorageKey = 'queryscope.history'
const maxHistoryEntries = 30

export type QueryHistoryEntry = { sql: string; timestamp: string; status: string }

function readHistory(): QueryHistoryEntry[] {
  try {
    const saved = JSON.parse(localStorage.getItem(historyStorageKey) ?? '[]')
    return Array.isArray(saved) ? saved.filter((item): item is QueryHistoryEntry => Boolean(item?.sql)).slice(0, maxHistoryEntries) : []
  } catch {
    return []
  }
}
export function useQueryHistory() {
  const [history, setHistory] = useState<QueryHistoryEntry[]>(readHistory)

  useEffect(() => {
    localStorage.setItem(historyStorageKey, JSON.stringify(history))
  }, [history])

  function addHistory(sql: string, status: string) {
    const trimmed = sql.trim()
    if (!trimmed) return
    setHistory((current) => {
      if (current[0]?.sql === trimmed) return [{ ...current[0], timestamp: new Date().toISOString(), status }, ...current.slice(1)]
      return [{ sql: trimmed, timestamp: new Date().toISOString(), status }, ...current].slice(0, maxHistoryEntries)
    })
  }

  function removeHistory(sql: string) {
    setHistory((current) => current.filter((entry) => entry.sql !== sql))
  }

  function clearHistory() {
    setHistory([])
  }

  return { history, addHistory, removeHistory, clearHistory }
}
