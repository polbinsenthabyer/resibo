/**
 * Where the bearer token lives between page loads.
 *
 * ⚠️ localStorage is readable by any script on the origin, so an XSS flaw
 * hands over the token. The alternative — an httpOnly cookie — needs the
 * backend to set it and brings CSRF back with it, and the API is deliberately
 * stateless with the token in the Authorization header (SecurityConfig).
 * Recorded as a scaffold decision to revisit alongside resibo-plan.md §1.3,
 * not as a settled answer.
 */

const TOKEN_KEY = 'resibo.accessToken'

/** Notified whenever the token changes, so the UI can react to a sign-out. */
type Listener = (token: string | null) => void

const listeners = new Set<Listener>()

export const tokenStorage = {
  get(): string | null {
    try {
      return window.localStorage.getItem(TOKEN_KEY)
    } catch {
      // Private browsing and some kiosk configurations deny localStorage.
      // Losing the token is survivable; crashing the app is not.
      return null
    }
  },

  set(token: string): void {
    try {
      window.localStorage.setItem(TOKEN_KEY, token)
    } catch {
      /* see get() */
    }
    listeners.forEach((listener) => listener(token))
  },

  clear(): void {
    try {
      window.localStorage.removeItem(TOKEN_KEY)
    } catch {
      /* see get() */
    }
    listeners.forEach((listener) => listener(null))
  },

  subscribe(listener: Listener): () => void {
    listeners.add(listener)
    return () => listeners.delete(listener)
  },
}
