import { requestJson } from './client'

export type BackendStatus = 'checking' | 'waking' | 'connected' | 'unavailable'

let inFlightHealthRequest: Promise<boolean> | null = null

function requestHealth(): Promise<boolean> {
  if (!inFlightHealthRequest) {
    inFlightHealthRequest = requestJson<{ status?: string }>('/api/health')
      .then((payload) => payload.status === 'ok')
      .catch(() => false)
      .finally(() => { inFlightHealthRequest = null })
  }
  return inFlightHealthRequest
}

export async function checkBackendHealth(signal?: AbortSignal): Promise<boolean> {
  const request = requestHealth()
  if (!signal) return request
  if (signal.aborted) return false

  return await new Promise<boolean>((resolve) => {
    let settled = false
    const settle = (value: boolean) => {
      if (settled) return
      settled = true
      signal.removeEventListener('abort', onAbort)
      resolve(value)
    }
    const onAbort = () => settle(false)
    signal.addEventListener('abort', onAbort, { once: true })
    void request.then(settle)
  })
}
