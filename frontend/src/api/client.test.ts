import { afterEach, describe, expect, it, vi } from 'vitest'
import { API_REQUEST_TIMEOUT_MS, ApiRequestError, requestJson } from './client'

afterEach(() => {
  vi.useRealTimers()
  vi.restoreAllMocks()
})

describe('requestJson', () => {
  it('aborts a cold-start request at the configured timeout with a useful message', async () => {
    vi.useFakeTimers()
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (_input, init) => await new Promise<Response>((_resolve, reject) => {
      init?.signal?.addEventListener('abort', () => reject(new DOMException('Aborted', 'AbortError')), { once: true })
    }))

    const request = requestJson('/api/health')
    const assertion = expect(request).rejects.toMatchObject({ kind: 'timeout', message: expect.stringMatching(/waking up after inactivity/i) })
    await vi.advanceTimersByTimeAsync(API_REQUEST_TIMEOUT_MS)

    await assertion
  })

  it('returns JSON and preserves HTTP errors for callers to classify', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(new Response(JSON.stringify({ ok: true }), { status: 200 }))
    await expect(requestJson<{ ok: boolean }>('/api/health')).resolves.toEqual({ ok: true })

    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(new Response(JSON.stringify({ error: 'Bad query' }), { status: 400 }))
    await expect(requestJson('/api/query/parse')).rejects.toEqual(expect.objectContaining({
      kind: 'http',
      status: 400,
      message: 'Bad query',
    } satisfies Partial<ApiRequestError>))
  })
})
