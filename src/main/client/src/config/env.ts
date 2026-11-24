/**
 * Build-time configuration, read once and validated here rather than through
 * scattered `import.meta.env` reads.
 *
 * Nothing in this file may hold a credential: Vite inlines every VITE_ value
 * into the shipped bundle, so anything here is public by construction.
 */

/**
 * Where the API lives.
 *
 * Defaults to the relative path `/api`, which works in two situations without
 * any configuration: `npm run dev`, where vite.config.ts proxies /api to
 * localhost:8083, and a production build served from the same origin as the
 * API. Set VITE_API_BASE_URL at build time when the two are on different
 * origins — e.g. VITE_API_BASE_URL=https://resibo-api.polbinsenthabyer.uk/api.
 */
const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? '/api').replace(/\/+$/, '')

export const env = {
  apiBaseUrl,
  isDevelopment: import.meta.env.DEV,
} as const
