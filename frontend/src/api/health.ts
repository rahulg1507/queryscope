export type BackendStatus = 'checking' | 'connected' | 'unavailable'

export async function checkBackendHealth(signal?: AbortSignal): Promise<boolean> {
  try {
    const response = await fetch('/api/health', { signal })
    if (!response.ok) return false

    const payload = (await response.json()) as { status?: string }
    return payload.status === 'ok'
  } catch {
    return false
  }
}
