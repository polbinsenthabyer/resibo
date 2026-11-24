import { tokenStorage } from '@/api/tokenStorage'

import type { UserRole } from './types'

/**
 * Read the role and expiry for client-side navigation only. This does not
 * verify the JWT signature; protected API requests are validated by the
 * server, which is the sole authority on access.
 */
export function roleFromToken(token: string | null): UserRole | null {
  if (!token) return null

  const payloadPart = token.split('.')[1]
  if (!payloadPart) return null

  try {
    const base64 = payloadPart.replace(/-/g, '+').replace(/_/g, '/')
    const payload: unknown = JSON.parse(atob(base64.padEnd(Math.ceil(base64.length / 4) * 4, '=')))
    if (typeof payload !== 'object' || payload === null) return null

    const claims = payload as { role?: unknown; exp?: unknown }
    if (claims.role !== 'ADMIN' && claims.role !== 'EMPLOYEE') return null
    if (typeof claims.exp !== 'number' || !Number.isFinite(claims.exp)) return null
    if (claims.exp * 1000 <= Date.now()) return null

    return claims.role
  } catch {
    return null
  }
}

export function getStoredRole(): UserRole | null {
  return roleFromToken(tokenStorage.get())
}
