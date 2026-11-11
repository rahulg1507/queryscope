import { apiUrl } from './config'

export const API_REQUEST_TIMEOUT_MS = 75_000

export type ApiRequestErrorKind = 'timeout' | 'network' | 'http'

export class ApiRequestError extends Error {
  readonly kind: ApiRequestErrorKind
  readonly status?: number

  constructor(message: string, kind: ApiRequestErrorKind, status?: number) {
    super(message)
    this.name = 'ApiRequestError'
    this.kind = kind
    this.status = status
  }
}

function timeoutMessage() {
  return 'The backend is waking up after inactivity. The first request can take around a minute; please retry if it does not recover.'
}

function networkMessage() {
  return 'Backend unavailable. Could not reach the QueryScope backend. Please retry.'
}

export async function requestJson<T>(path: string, init: RequestInit = {}, timeoutMs = API_REQUEST_TIMEOUT_MS): Promise<T> {
  const controller = new AbortController()
  let timedOut = false
  const timeoutId = setTimeout(() => {
    timedOut = true
    controller.abort()
  }, timeoutMs)
  const externalSignal = init.signal
  const abortFromCaller = () => controller.abort()

  if (externalSignal) {
    if (externalSignal.aborted) controller.abort()
    else externalSignal.addEventListener('abort', abortFromCaller, { once: true })
  }

  try {
    const response = await fetch(apiUrl(path), { ...init, signal: controller.signal })
    const text = await response.text()
    const payload = text ? JSON.parse(text) as T & { error?: string } : null
    if (!response.ok) {
      throw new ApiRequestError(payload?.error ?? `Request failed with status ${response.status}.`, 'http', response.status)
    }
    return payload as T
  } catch (error) {
    if (error instanceof ApiRequestError) throw error
    if (externalSignal?.aborted) throw error
    if (timedOut) throw new ApiRequestError(timeoutMessage(), 'timeout')
    throw new ApiRequestError(networkMessage(), 'network')
  } finally {
    clearTimeout(timeoutId)
    externalSignal?.removeEventListener('abort', abortFromCaller)
  }
}
