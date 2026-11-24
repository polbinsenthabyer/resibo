import { StatusMessage } from '@/components/feedback/StatusMessage'
import { Button } from '@/components/ui/Button'
import { env } from '@/config/env'

import { useHealthCheck } from '../hooks/useHealthCheck'

import styles from './HealthPage.module.scss'

/**
 * Proves the whole chain works: route → page → hook → API client → backend.
 *
 * It exists because it is the one call that needs no token, so a fresh
 * checkout can confirm the frontend really is talking to the API before any
 * feature work starts. Replace it with a real landing page once there is one.
 */
export function HealthPage() {
  const { status, error, run } = useHealthCheck()

  return (
    <section className={styles.page}>
      <h2>API status</h2>

      {status === 'idle' && <StatusMessage tone="pending">Not checked yet.</StatusMessage>}
      {status === 'loading' && <StatusMessage tone="pending">Checking the API…</StatusMessage>}
      {status === 'success' && (
        <StatusMessage tone="success">The API answered. The connection works.</StatusMessage>
      )}
      {status === 'error' && (
        <StatusMessage tone="failure" error={error}>
          {error.message}
        </StatusMessage>
      )}

      <Button variant="primary" onClick={() => void run()} disabled={status === 'loading'}>
        Check again
      </Button>

      <p className={styles.detail}>
        Calling <code>{env.apiBaseUrl}/health/check</code>
      </p>
    </section>
  )
}
