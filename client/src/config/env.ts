/**
 * Build-time configuration, read once and validated here rather than through
 * scattered `import.meta.env` reads.
 *
 * Nothing in this file may hold a credential: Vite inlines every VITE_ value
 * into the shipped bundle, so anything here is public by construction.
 */

/**
 * The production bundle uses the deployed API. Local development uses the
 * `/api` Vite proxy instead, so it does not call the public API by default.
 * This is not database isolation: the local backend may still use the shared
 * live database. Set VITE_API_BASE_URL explicitly for another target.
 */
const DEPLOYED_API_BASE_URL = 'https://resibo-api.polbinsenthabyer.uk/api'

/**
 * Where the API lives.
 *
 * Override with `VITE_API_BASE_URL` at build or dev time. The one you are
 * most likely to want:
 *
 *   VITE_API_BASE_URL=/api
 *
 * which is a same-origin relative path — `vite.config.ts` proxies `/api` to a
 * local backend on :8083 in development. The production site serves the API
 * from its separate hostname, so the deployed URL remains its default.
 */
const defaultApiBaseUrl = import.meta.env.DEV ? '/api' : DEPLOYED_API_BASE_URL
const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? defaultApiBaseUrl).replace(/\/+$/, '')

export const env = {
  apiBaseUrl,
  isDevelopment: import.meta.env.DEV,
  /** True when pointed at the deployed API rather than a local backend. */
  isUsingDeployedApi: apiBaseUrl === DEPLOYED_API_BASE_URL,
} as const
