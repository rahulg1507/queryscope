import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { BackendStatus } from './BackendStatus'

describe('BackendStatus', () => {
  it('places the waking explanation below the compact badge', () => {
    render(<BackendStatus status="waking" onRetry={vi.fn()} />)
    const status = screen.getByRole('status')
    const pill = status.querySelector('.status-pill')
    const message = screen.getByText('First request after inactivity may take around a minute.')

    expect(pill).toHaveTextContent('Backend waking up')
    expect(pill).not.toContainElement(message)
    expect(message).toHaveClass('status-detail')
  })

  it('keeps Retry available for an unavailable backend', async () => {
    const onRetry = vi.fn()
    render(<BackendStatus status="unavailable" onRetry={onRetry} />)

    await userEvent.click(screen.getByRole('button', { name: 'Retry' }))
    expect(onRetry).toHaveBeenCalledOnce()
  })
})
