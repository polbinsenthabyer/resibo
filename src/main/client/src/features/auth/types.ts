/** Mirrors com.api.resibo.types.UserRoleType. */
export type UserRole = 'ADMIN' | 'EMPLOYEE'

/** Request body for POST /api/auth/login. */
export interface LoginRequest {
  username: string
  password: string
}

/**
 * `data` of a successful login — com.api.resibo.dtos.LoginResponseDto.
 *
 * No expiry is returned: the field was removed in 4883414. The token's own
 * `exp` claim is the only authority on when it stops working, and the backend
 * default is eight hours (jwt.expiration).
 */
export interface LoginResponse {
  username: string
  role: UserRole
  token: string
}

/**
 * Request body for POST /api/auth/signup.
 *
 * `role` is honoured only for an authenticated ADMIN, or for the very first
 * account on an empty system. An anonymous signup to a system that already has
 * users always becomes EMPLOYEE whatever is sent.
 */
export interface SignupRequest {
  username: string
  password: string
  name?: string
  role?: UserRole
}
