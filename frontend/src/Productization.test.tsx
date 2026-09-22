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
  it('navigates between the remaining public routes', async () => {
    window.history.pushState({}, '', '/docs')
    const user = userEvent.setup()
    render(<App />)
    expect(screen.getByRole('heading', { name: 'Docs' })).toBeInTheDocument()
    await user.click(screen.getAllByRole('link', { name: /Benchmarks/ })[0])
    expect(screen.getByRole('heading', { name: 'Benchmarks' })).toBeInTheDocument()
    await user.click(screen.getAllByRole('link', { name: /Workspace/ })[0])
    expect(screen.getByRole('heading', { name: 'Query workspace' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /Roadmap/ })).not.toBeInTheDocument()
  })

  it('falls back to Workspace for the removed roadmap URL', () => {
    window.history.pushState({}, '', '/roadmap')
    render(<App />)

    expect(screen.getByRole('heading', { name: 'Query workspace' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /Roadmap/ })).not.toBeInTheDocument()
  })

  it('opens accessible Help and Settings dialogs and closes them with Escape', async () => {
    window.history.pushState({}, '', '/workspace')
    const user = userEvent.setup()
    render(<App />)
    await user.click(screen.getByRole('button', { name: 'Open help' }))
    expect(screen.getByRole('dialog', { name: 'QueryScope help' })).toBeInTheDocument()
    expect(screen.getByText('KEY SHORTCUT')).toBeInTheDocument()
    await user.keyboard('{Escape}')
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Open workspace settings' }))
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
