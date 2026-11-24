/// <reference types="vite/client" />

/**
 * Typed build-time configuration. Merges with Vite's own ImportMetaEnv.
 *
 * Only VITE_-prefixed values are exposed to the bundle, and everything here is
 * compiled into the JavaScript the browser downloads — so a secret must never
 * be added to this interface. See resibo-plan.md §8.
 */
interface ImportMetaEnv {
  /** Base URL for the API, including the /api prefix. Optional — see config/env.ts. */
  readonly VITE_API_BASE_URL?: string
}
