import { endpoints } from '@/api/endpoints'
import { http } from '@/api/http'

/**
 * The API's own liveness check — distinct from /actuator/health, which reports
 * infrastructure state. This one proves the request path and the response
 * envelope are working, which is why the scaffold uses it: it is the only
 * endpoint that answers without a token.
 */
export const healthApi = {
  /**
   * Returns nothing on success. The endpoint's value is in the envelope around
   * it, not in `data` — a resolved promise means the API answered correctly.
   */
  async check(signal?: AbortSignal): Promise<void> {
    await http.get<null>(endpoints.health.check, {
      authenticated: false,
      feedback: 'none',
      ...(signal ? { signal } : {}),
    })
  },
}
