import { endpoints } from '@/api/endpoints'
import { http } from '@/api/http'
import { tokenStorage } from '@/api/tokenStorage'
import { notify } from '@/lib/notifications'

import { getStoredRole } from './session'
import type { LoginRequest, LoginResponse, SignupRequest } from './types'

/**
 * Auth calls. These are the only endpoints under /api that take no token, so
 * every request here passes `authenticated: false` — sending a stale token to
 * a public endpoint is harmless but misleading in a network log.
 */
export const authApi = {
  /** Signs in and stores the token, so later requests are authenticated. */
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    let successMessage = ''
    const result = await http.post<LoginResponse>(endpoints.auth.login, credentials, {
      authenticated: false,
      feedback: 'errors',
      onSuccessMessage: (message) => {
        successMessage = message
      },
    })
    tokenStorage.set(result.token)
    if (tokenStorage.get() !== result.token) {
      throw new Error(
        'This browser could not save your login. Please enable local storage and try again.',
      )
    }
    if (getStoredRole() !== result.role) {
      tokenStorage.clear()
      throw new Error('The login token could not be used. Please try again.')
    }
    notify.success(successMessage || 'Signed in successfully.')
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
