/**
 * The backend's response contract. Mirrors com.api.resibo.dtos.ResponseDto and
 * PageDto — if those change, this file is the first thing that has to follow.
 *
 * The shape is unusual and worth understanding before calling anything:
 * **a failed request normally still returns HTTP 200.** Business failures are
 * reported by `responseType: 'ERROR'` plus a code in the envelope, not by a
 * status code. The only genuine HTTP error statuses the API returns are 401
 * and 403, from the security layer. So HTTP status alone never tells you
 * whether a call succeeded — which is exactly why every request goes through
 * `request()` in http.ts rather than calling fetch directly.
 */

export type ResponseType = 'SUCCESS' | 'ERROR'

export interface ResponseDto<TData = unknown> {
  responseType: ResponseType
  code: string
  data: TData | null
  message: string
  /**
   * Internal class and method of whatever built the response.
   *
   * Returned on every response, errors included, and currently live on the
   * public API — tracked as resibo-plan.md §12 #10. Treat as diagnostic noise:
   * never branch on it, never show it to a user.
   */
  className: string | null
  methodName: string | null
  /**
   * Server timestamp, `yyyy-MM-dd HH:mm:ss.SSS`.
   *
   * ⚠️ Serialised in **UTC**, not local time — a known defect held open while
   * the project's timezone is undecided (resibo-plan.md §10, §12 #8). Do not
   * render this to a user until that question is answered.
   */
  responseTime: string
}

/** Pagination envelope carried in `data` by every listing endpoint. */
export interface PageDto<TItem> {
  items: TItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  hasNext: boolean
  hasPrevious: boolean
}

/**
 * Response codes the frontend is expected to react to differently from a plain
 * failure. Everything else is shown using the envelope's own `message`.
 *
 * Kept deliberately short: a frontend that enumerates every backend code goes
 * stale the moment one is added. Add a code here only when the UI must do
 * something specific for it.
 */
export const ResponseCode = {
  SUCCESS: 'S0001',
  UNAUTHENTICATED: 'E0401',
  NO_PERMISSION: 'E0403',
  INVALID_CREDENTIALS: 'E0106',
  USERNAME_ALREADY_EXISTS: 'E0104',
} as const

export type ResponseCodeValue = (typeof ResponseCode)[keyof typeof ResponseCode]
