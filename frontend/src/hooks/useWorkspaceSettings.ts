import { useEffect, useState } from 'react'

export const settingsStorageKey = 'queryscope.settings'

export type WorkspaceSettings = {
  density: 'comfortable' | 'compact'
  showOptimizerDetails: boolean
  showExecutionMetrics: boolean
  autoExpandPlan: boolean
  restoreLastQuery: boolean
}
export const defaultWorkspaceSettings: WorkspaceSettings = {
  density: 'comfortable',
  showOptimizerDetails: true,
  showExecutionMetrics: true,
  autoExpandPlan: true,
  restoreLastQuery: true,
}

function readSettings(): WorkspaceSettings {
  try {
    const saved = JSON.parse(localStorage.getItem(settingsStorageKey) ?? '{}') as Partial<WorkspaceSettings>
    return { ...defaultWorkspaceSettings, ...saved, density: saved.density === 'compact' ? 'compact' : 'comfortable' }
  } catch {
    return defaultWorkspaceSettings
  }
}

export function useWorkspaceSettings() {
  const [settings, setSettings] = useState<WorkspaceSettings>(readSettings)

  useEffect(() => {
    localStorage.setItem(settingsStorageKey, JSON.stringify(settings))
  }, [settings])

  function updateSettings(changes: Partial<WorkspaceSettings>) {
    setSettings((current) => ({ ...current, ...changes }))
  }

  function resetSettings() {
    setSettings(defaultWorkspaceSettings)
  }

  return { settings, updateSettings, resetSettings }
}
