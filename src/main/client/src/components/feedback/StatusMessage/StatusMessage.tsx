import type { ReactNode } from 'react'

import { ApiError } from '@/api/errors'
import { cx } from '@/lib/classNames'

import styles from './StatusMessage.module.scss'

type Tone = 'pending' | 'success' | 'failure'

interface StatusMessageProps {
  tone: Tone
  children: ReactNode
  /**
   * Shown beneath the message as the backend's response code, when there is
   * one. Useful while building; a finished screen should usually show the
   * message alone.
   */
  error?: Error | null
}

/**
 * A short status line for a request in flight, succeeded, or failed.
 *
 * `role="status"` so a screen reader announces the change without the user
 * having to go looking for it.
 */
export function StatusMessage({ tone, children, error = null }: StatusMessageProps) {
  const code = error instanceof ApiError ? error.code : null

  return (
    <p
      className={cx(
        styles.message,
        tone === 'pending' && styles.pending,
        tone === 'success' && styles.success,
        tone === 'failure' && styles.failure,
      )}
      role="status"
    >
      {children}
      {code && <code className={styles.code}>{code}</code>}
    </p>
  )
}
