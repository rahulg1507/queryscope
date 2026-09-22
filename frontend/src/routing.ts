export const routes = ['/workspace', '/docs', '/benchmarks'] as const
export type Route = typeof routes[number]

export function routeFromLocation(): Route {
  const pathname = window.location.pathname.replace(/\/$/, '') || '/workspace'
  return routes.includes(pathname as Route) ? pathname as Route : '/workspace'
}
export function navigateTo(route: Route) {
  window.history.pushState({}, '', route)
  window.dispatchEvent(new PopStateEvent('popstate'))
}
