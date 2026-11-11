import { afterEach, describe, expect, it, vi } from 'vitest'
import { checkBackendHealth } from './health'

afterEach(() => vi.restoreAllMocks())

describe('checkBackendHealth', () => {
  it('deduplicates simultaneous health checks', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify({ status: 'ok' }), { status: 200 }))

    await expect(Promise.all([checkBackendHealth(), checkBackendHealth()])).resolves.toEqual([true, true])
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })
})
