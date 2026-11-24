/**
 * Build-time configuration, read once and validated here rather than through
 * scattered `import.meta.env` reads.
 *
 * Nothing in this file may hold a credential: Vite inlines every VITE_ value
 * into the shipped bundle, so anything here is public by construction.
 */

/**
 * The deployed API. This is the default so that `npm run dev` works with
 * nothing else running — no local backend, no database, no JAVA_HOME.
 *
 * ⚠️ **That means the development UI talks to production by default.** Today
 * that is harmless: the only call the app makes is a GET health check. It
 * stops being harmless the moment a screen can write — a product form would
 * edit the live catalog from a developer's laptop with no warning. Before the
 * first mutating screen ships, either flip this default back to the proxy or
 * make the target obvious in the UI. Tracked in the vault's `Tasks`.
 *
 * CORS: the API allows this origin. `CORS_ALLOWED_ORIGINS` defaults to `*` and
 * names `http://localhost:5173` explicitly (backend `1ad0803`), and the
 * preflight was checked against the live host rather than assumed.
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
 * local backend on :8083, and it is also correct for a production build served
 * from the same origin as the API. Use that when running against your own
 * backend; CORS never enters the picture because the browser sees one origin.
 */
const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? DEPLOYED_API_BASE_URL).replace(/\/+$/, '')

export const env = {
  apiBaseUrl,
  isDevelopment: import.meta.env.DEV,
  /** True when pointed at the deployed API rather than a local backend. */
  isUsingDeployedApi: apiBaseUrl === DEPLOYED_API_BASE_URL,
} as const
