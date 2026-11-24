import { endpoints } from '@/api/endpoints'
import { http } from '@/api/http'
import { tokenStorage } from '@/api/tokenStorage'

import type { LoginRequest, LoginResponse, SignupRequest } from './types'

/**
 * Auth calls. These are the only endpoints under /api that take no token, so
 * every request here passes `authenticated: false` — sending a stale token to
 * a public endpoint is harmless but misleading in a network log.
 */
export const authApi = {
  /** Signs in and stores the token, so later requests are authenticated. */
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    const result = await http.post<LoginResponse>(endpoints.auth.login, credentials, {
      authenticated: false,
    })
    tokenStorage.set(result.token)
    return result
  },

  /**
   * Registers an account. Returns nothing: the backend confirms the signup
   * without disclosing the created account, and does not sign the caller in.
   */
  async signup(details: SignupRequest): Promise<void> {
    await http.post<null>(endpoints.auth.signup, details, { authenticated: false })
  },

  /** Local only — the API is stateless, so there is nothing to tell it. */
  logout(): void {
    tokenStorage.clear()
  },
}
