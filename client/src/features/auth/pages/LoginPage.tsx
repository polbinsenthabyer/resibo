import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'

import { paths } from '@/app/paths'
import { tokenStorage } from '@/api/tokenStorage'
import { StatusMessage } from '@/components/feedback/StatusMessage'
import { Button } from '@/components/ui/Button'
import { env } from '@/config/env'

import { authApi } from '../api'
import type { LoginResponse } from '../types'

import styles from './AuthPage.module.scss'

export function LoginPage() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<Error | null>(null)
  const [login, setLogin] = useState<LoginResponse | null>(null)
  const [hasSavedToken, setHasSavedToken] = useState(() => tokenStorage.get() !== null)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (isSubmitting) return

    setIsSubmitting(true)
    setError(null)
    setLogin(null)

    try {
      const result = await authApi.login({ username: username.trim(), password })
      setLogin(result)
      setHasSavedToken(true)
      setPassword('')
    } catch (cause) {
      setError(cause instanceof Error ? cause : new Error('Could not sign in. Please try again.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  function handleSignOut() {
    authApi.logout()
    setHasSavedToken(false)
    setLogin(null)
    setError(null)
  }

  return (
    <section className={styles.page} aria-labelledby="login-title">
      <h2 id="login-title">Log in</h2>
      <p className={styles.intro}>Use your Resibo account to sign in.</p>

      <form className={styles.form} onSubmit={(event) => void handleSubmit(event)}>
        <div className={styles.field}>
          <label className={styles.label} htmlFor="login-username">
            Username
          </label>
          <input
            className={styles.input}
            id="login-username"
            name="username"
            type="text"
            autoComplete="username"
            maxLength={100}
            required
            disabled={isSubmitting}
            value={username}
            onChange={(event) => setUsername(event.target.value)}
          />
        </div>

        <div className={styles.field}>
          <label className={styles.label} htmlFor="login-password">
            Password
          </label>
          <input
            className={styles.input}
            id="login-password"
            name="password"
            type="password"
            autoComplete="current-password"
            required
            disabled={isSubmitting}
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
        </div>

        {error && (
          <StatusMessage tone="failure" error={error}>
            {error.message}
          </StatusMessage>
        )}
        {login && (
          <StatusMessage tone="success">
            Signed in as {login.username} ({login.role === 'ADMIN' ? 'administrator' : 'employee'}).
          </StatusMessage>
        )}
        {!login && hasSavedToken && (
          <StatusMessage tone="pending">
            A login token is saved on this device. You can sign in again or sign out.
          </StatusMessage>
        )}

        <div className={styles.actions}>
          <Button type="submit" variant="primary" disabled={isSubmitting}>
            {isSubmitting ? 'Signing in…' : 'Log in'}
          </Button>
          {hasSavedToken && (
            <Button onClick={handleSignOut} disabled={isSubmitting}>
              Sign out
            </Button>
          )}
        </div>
      </form>

      <p className={styles.switchPage}>
        New to Resibo? <Link to={paths.signup}>Create an account</Link>
      </p>
      <p className={styles.apiTarget}>
        API target: <code>{env.apiBaseUrl}</code>
      </p>
    </section>
  )
}
