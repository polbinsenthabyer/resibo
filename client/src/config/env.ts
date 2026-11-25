/**
 * Build-time configuration, read once and validated here rather than through
 * scattered `import.meta.env` reads.
 *
 * Nothing in this file may hold a credential: Vite inlines every VITE_ value
 * into the shipped bundle, so anything here is public by construction.
 */

/**
 * Both production and local development use the deployed API by default.
 * The owner explicitly chose this for local preview. Signup can write to the
 * live service, so the development signup page warns before submission.
 * Set VITE_API_BASE_URL=/api to use the local Vite proxy instead.
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
 * local backend on :8083 in development. The deployed URL is the default in
 * both dev and production builds.
 */
const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? DEPLOYED_API_BASE_URL).replace(/\/+$/, '')

export const env = {
  apiBaseUrl,
  isDevelopment: import.meta.env.DEV,
  /** True when pointed at the deployed API rather than a local backend. */
  isUsingDeployedApi: apiBaseUrl === DEPLOYED_API_BASE_URL,
} as const
