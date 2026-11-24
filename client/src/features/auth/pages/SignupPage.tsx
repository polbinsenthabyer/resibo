import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'

import { paths } from '@/app/paths'
import { StatusMessage } from '@/components/feedback/StatusMessage'
import { Button } from '@/components/ui/Button'
import { env } from '@/config/env'

import { authApi } from '../api'

import styles from './AuthPage.module.scss'

export function SignupPage() {
  const [username, setUsername] = useState('')
  const [name, setName] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<Error | null>(null)
  const [created, setCreated] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (isSubmitting) return

    setError(null)
    if (password !== confirmPassword) {
      setError(new Error('Passwords do not match.'))
      return
    }

    setIsSubmitting(true)
    try {
      const trimmedName = name.trim()
      await authApi.signup({
        username: username.trim(),
        password,
        ...(trimmedName ? { name: trimmedName } : {}),
      })
      setCreated(true)
      setPassword('')
      setConfirmPassword('')
    } catch (cause) {
      setError(
        cause instanceof Error
          ? cause
          : new Error('Could not create the account. Please try again.'),
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section className={styles.page} aria-labelledby="signup-title">
      <h2 id="signup-title">Create an account</h2>
      <p className={styles.intro}>Enter your details to request a Resibo account.</p>

      {env.isDevelopment && (
        <StatusMessage tone="pending">
          {env.isUsingDeployedApi
            ? 'This form creates an account on the deployed API.'
            : 'The local backend currently uses the shared live database. Creating an account here writes there.'}
        </StatusMessage>
      )}

      {created ? (
        <div className={styles.form}>
          <StatusMessage tone="success">Account created. You can now log in.</StatusMessage>
          <Link to={paths.login}>Go to login</Link>
        </div>
      ) : (
        <form className={styles.form} onSubmit={(event) => void handleSubmit(event)}>
          <div className={styles.field}>
            <label className={styles.label} htmlFor="signup-username">
              Username
            </label>
            <input
              className={styles.input}
              id="signup-username"
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
            <label className={styles.label} htmlFor="signup-name">
              Name (optional)
            </label>
            <input
              className={styles.input}
              id="signup-name"
              name="name"
              type="text"
              autoComplete="name"
              maxLength={100}
              disabled={isSubmitting}
              value={name}
              onChange={(event) => setName(event.target.value)}
            />
          </div>

          <div className={styles.field}>
            <label className={styles.label} htmlFor="signup-password">
              Password
            </label>
            <input
              className={styles.input}
              id="signup-password"
              name="password"
              type="password"
              autoComplete="new-password"
              minLength={8}
              required
              disabled={isSubmitting}
              value={password}
              onChange={(event) => setPassword(event.target.value)}
            />
          </div>

          <div className={styles.field}>
            <label className={styles.label} htmlFor="signup-confirm-password">
              Confirm password
            </label>
            <input
              className={styles.input}
              id="signup-confirm-password"
              name="confirm-password"
              type="password"
              autoComplete="new-password"
              minLength={8}
              required
              disabled={isSubmitting}
              value={confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)}
            />
          </div>

          {error && (
            <StatusMessage tone="failure" error={error}>
              {error.message}
            </StatusMessage>
          )}

          <Button type="submit" variant="primary" disabled={isSubmitting}>
            {isSubmitting ? 'Creating account…' : 'Sign up'}
          </Button>
        </form>
      )}

      {!created && (
        <p className={styles.switchPage}>
          Already have an account? <Link to={paths.login}>Log in</Link>
        </p>
      )}
      <p className={styles.apiTarget}>
        API target: <code>{env.apiBaseUrl}</code>
      </p>
    </section>
  )
}
