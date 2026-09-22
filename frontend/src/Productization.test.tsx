import { afterEach, describe, expect, it, vi } from 'vitest'
import userEvent from '@testing-library/user-event'
import { cleanup, render, screen } from '@testing-library/react'
import App from './App'

afterEach(() => {
  cleanup()
  localStorage.clear()
  window.history.pushState({}, '', '/workspace')
  vi.restoreAllMocks()
})

describe('productized workspace navigation', () => {
  it('navigates between the real public routes', async () => {
    window.history.pushState({}, '', '/docs')
    const user = userEvent.setup()
    render(<App />)
    expect(screen.getByRole('heading', { name: 'Docs' })).toBeInTheDocument()
    await user.click(screen.getAllByRole('link', { name: /Benchmarks/ })[0])
    expect(screen.getByRole('heading', { name: 'Benchmarks' })).toBeInTheDocument()
    await user.click(screen.getAllByRole('link', { name: /Roadmap/ })[0])
    expect(screen.getByRole('heading', { name: 'Roadmap' })).toBeInTheDocument()
  })

  it('opens accessible Help and Settings dialogs and closes them with Escape', async () => {
    window.history.pushState({}, '', '/roadmap')
    const user = userEvent.setup()
    render(<App />)
    await user.click(screen.getAllByRole('button', { name: 'Help' }).find((button) => button.hasAttribute('aria-label'))!)
    expect(screen.getByRole('dialog', { name: 'QueryScope help' })).toBeInTheDocument()
    expect(screen.getByText('Keyboard shortcuts')).toBeInTheDocument()
    await user.keyboard('{Escape}')
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    await user.click(screen.getAllByRole('button', { name: 'Settings' }).find((button) => button.hasAttribute('aria-label'))!)
    expect(screen.getByRole('dialog', { name: 'Workspace settings' })).toBeInTheDocument()
    await user.selectOptions(screen.getByLabelText('Density'), 'compact')
    expect(JSON.parse(localStorage.getItem('queryscope.settings') ?? '{}')).toMatchObject({ density: 'compact' })
    await user.keyboard('{Escape}')
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('loads documentation examples into Workspace without executing them', async () => {
    window.history.pushState({}, '', '/docs')
    const user = userEvent.setup()
    render(<App />)
    await user.click(screen.getAllByRole('button', { name: 'Use in Workspace' })[0])
    expect(screen.getByRole('heading', { name: 'Query workspace' })).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: 'SQL query' })).toHaveValue('SELECT name, age\nFROM users\nWHERE age > 18;')
  })
})
