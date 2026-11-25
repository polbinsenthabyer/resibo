import { env } from '@/config/env'
import { notify } from '@/lib/notifications'

import { ApiError, NetworkError } from './errors'
import { tokenStorage } from './tokenStorage'
import type { ResponseDto } from './types'

/**
 * The one place a network request is made.
 *
 * Every call goes through here so that the envelope contract in types.ts is
 * applied in exactly one place: a caller gets the unwrapped `data` on success
 * and a thrown ApiError on failure, and never has to remember that this API
 * reports failure with HTTP 200.
 */

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  /** Serialised as JSON. Omit for GET and DELETE. */
  body?: unknown
  /** Attach the stored bearer token. Default true; false for /api/auth/**. */
  authenticated?: boolean
  signal?: AbortSignal
  /** Mutations show backend success and errors; reads show errors by default. */
  feedback?: 'all' | 'errors' | 'none'
  /** Lets a caller show success only after local follow-up work completes. */
  onSuccessMessage?: (message: string) => void
}

async function request<TData>(path: string, options: RequestOptions = {}): Promise<TData> {
  const { method = 'GET', body, authenticated = true, signal, onSuccessMessage } = options
  const feedback = options.feedback ?? (method === 'GET' ? 'errors' : 'all')
  const fail = (error: Error): never => {
    if (feedback !== 'none') notify.error(error.message)
    throw error
  }

  const headers = new Headers({ Accept: 'application/json' })
  if (body !== undefined) {
    headers.set('Content-Type', 'application/json')
  }

  if (authenticated) {
    const token = tokenStorage.get()
    if (token) {
      headers.set('Authorization', `Bearer ${token}`)
    }
  }

  let response: Response
  try {
    response = await fetch(`${env.apiBaseUrl}${path}`, {
      method,
      headers,
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
      ...(signal ? { signal } : {}),
    })
  } catch (cause) {
    // fetch only rejects when the request never completed. An HTTP error
    // status resolves normally and is handled below.
    if (cause instanceof DOMException && cause.name === 'AbortError') {
      throw cause
    }
    return fail(new NetworkError('Could not reach the Resibo API.', cause))
  }

  const envelope = await readEnvelope<TData>(response)

  // 401 and 403 are the only real error statuses the API returns, and they
  // carry the envelope too — so this is a safety net for anything else that
  // answers with a status but no recognisable body (a proxy, a gateway).
  if (envelope === null) {
    if (!response.ok) {
      return fail(
        new ApiError(
          `The API returned ${response.status} without a response body.`,
          `HTTP_${response.status}`,
          response.status,
        ),
      )
    }
    return fail(new NetworkError('The API returned a response that was not the expected envelope.'))
  }

  if (envelope.responseType === 'ERROR') {
    return fail(
      new ApiError(
        envelope.message.trim() || 'The request failed. Please try again.',
        envelope.code,
        response.status,
      ),
    )
  }

  // `data` is null for endpoints that only confirm an action — signup, delete.
  // The call site's type parameter decides whether that is meaningful, so the
  // null is passed through rather than being turned into an error here.
  if (envelope.message) {
    onSuccessMessage?.(envelope.message)
    if (feedback === 'all') notify.success(envelope.message)
  }
  return envelope.data as TData
}

async function readEnvelope<TData>(response: Response): Promise<ResponseDto<TData> | null> {
  const text = await response.text()
  if (!text) {
    return null
  }

  let parsed: unknown
  try {
    parsed = JSON.parse(text)
  } catch {
    return null
  }

  return isResponseDto(parsed) ? (parsed as ResponseDto<TData>) : null
}

/** Structural check — the envelope is the contract, not the status code. */
function isResponseDto(value: unknown): boolean {
  if (typeof value !== 'object' || value === null) {
    return false
  }
  const candidate = value as Partial<ResponseDto>
  return (
    (candidate.responseType === 'SUCCESS' || candidate.responseType === 'ERROR') &&
    typeof candidate.code === 'string' &&
    typeof candidate.message === 'string'
  )
}

export const http = {
  get: <TData>(path: string, options?: Omit<RequestOptions, 'method' | 'body'>) =>
    request<TData>(path, { ...options, method: 'GET' }),

  post: <TData>(path: string, body?: unknown, options?: Omit<RequestOptions, 'method' | 'body'>) =>
    request<TData>(path, { ...options, method: 'POST', ...(body === undefined ? {} : { body }) }),

  delete: <TData>(path: string, options?: Omit<RequestOptions, 'method' | 'body'>) =>
    request<TData>(path, { ...options, method: 'DELETE' }),
}
