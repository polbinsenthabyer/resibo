import { ResponseCode } from './types'

/**
 * A request that did not succeed, whether the backend said so in the envelope
 * (HTTP 200 with responseType ERROR) or at the HTTP layer (401 / 403).
 *
 * Both are the same thing to a caller — the operation did not happen — so they
 * arrive as one error type with the distinction preserved in `httpStatus` for
 * the cases that genuinely need it.
 */
export class ApiError extends Error {
  readonly code: string
  readonly httpStatus: number

  constructor(message: string, code: string, httpStatus: number) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.httpStatus = httpStatus
  }

  /** No token, or one the server would not accept. Signing in again may help. */
  get isUnauthenticated(): boolean {
    return this.httpStatus === 401 || this.code === ResponseCode.UNAUTHENTICATED
  }

  /** A valid token without the required role. Signing in again will not help. */
  get isForbidden(): boolean {
    return this.httpStatus === 403 || this.code === ResponseCode.NO_PERMISSION
  }
}

/**
 * The network never reached the API, or the response was not the envelope at
 * all — a proxy error page, a dropped connection, the backend not running.
 *
 * Separate from ApiError because the remedy is different: nothing about the
 * request was rejected, so retrying is reasonable.
 */
export class NetworkError extends Error {
  constructor(message: string, cause?: unknown) {
    // `cause` goes through the standard Error options bag rather than a
    // declared field, so it behaves like any other Error cause in a devtools
    // stack trace instead of being a look-alike property.
    super(message, { cause })
    this.name = 'NetworkError'
  }
}
