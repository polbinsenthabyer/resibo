import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'

import { ApiError } from '@/api/errors'
import { userApi } from '@/api/users/api'
import type { ManagedUserRole, UserUpsertRequest } from '@/api/users/types'
import { paths } from '@/app/paths'
import { IconButton } from '@/components/ui/IconButton/IconButton'

import { useAdminModal } from '../hooks/useAdminModal'
import styles from '../pages/AdminPage.module.scss'

interface UserEditorProps {
  userId: number | null
  onClose: () => void
  onSaved: () => void
}

interface FormValues {
  username: string
  name: string
  password: string
  role: '' | ManagedUserRole
}

const emptyForm: FormValues = { username: '', name: '', password: '', role: 'EMPLOYEE' }

export function UserEditor({ userId, onClose, onSaved }: UserEditorProps) {
  const dialogRef = useRef<HTMLElement>(null)
  const usernameRef = useRef<HTMLInputElement>(null)
  const [values, setValues] = useState<FormValues>(emptyForm)
  const [loadState, setLoadState] = useState<'ready' | 'loading' | 'error'>(
    userId === null ? 'ready' : 'loading',
  )
  const [reloadKey, setReloadKey] = useState(0)
  const [loadError, setLoadError] = useState<Error | null>(null)
  const [formError, setFormError] = useState<string | null>(null)
  const [isSaving, setIsSaving] = useState(false)

  useAdminModal(dialogRef, onClose, isSaving)

  useEffect(() => {
    if (loadState === 'ready') usernameRef.current?.focus()
  }, [loadState])

  useEffect(() => {
    if (userId === null) return
    const controller = new AbortController()
    userApi.getById(userId, controller.signal).then(
      (user) => {
        if (controller.signal.aborted) return
        setValues({
          username: user.username,
          name: user.name ?? '',
          password: '',
          role: '',
        })
        setLoadState('ready')
      },
      (cause) => {
        if (controller.signal.aborted) return
        setLoadError(cause instanceof Error ? cause : new Error('Could not load user.'))
        setLoadState('error')
      },
    )
    return () => controller.abort()
  }, [userId, reloadKey])

  function update(field: keyof FormValues, value: string) {
    setFormError(null)
    setValues((current) => ({ ...current, [field]: value }))
  }

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (isSaving) return

    const username = values.username.trim()
    const name = values.name.trim()
    if (!username || username.length > 100) {
      setFormError('Enter a username of at most 100 characters.')
      return
    }
    if (name.length > 100) {
      setFormError('Keep the name within 100 characters.')
      return
    }
    if (userId === null && (!values.password.trim() || values.password.length < 8)) {
      setFormError('Enter a password of at least 8 characters.')
      return
    }
    if (
      userId !== null &&
      values.password &&
      (!values.password.trim() || values.password.length < 8)
    ) {
      setFormError('A new password needs at least 8 characters.')
      return
    }

    const payload: UserUpsertRequest = {
      ...(userId === null ? {} : { id: userId }),
      username,
      name: name || null,
      ...(values.password ? { password: values.password } : {}),
      ...(values.role ? { role: values.role } : {}),
    }

    setFormError(null)
    setIsSaving(true)
    try {
      await userApi.save(payload)
      onSaved()
    } catch (cause) {
      setFormError(cause instanceof Error ? cause.message : 'Could not save user.')
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <div className={styles.editorOverlay}>
      <section
        ref={dialogRef}
        className={styles.editor}
        role="dialog"
        aria-modal="true"
        aria-labelledby="user-editor-title"
        tabIndex={-1}
      >
        <div className={styles.sectionHeading}>
          <div>
            <p className={styles.eyebrow}>{userId === null ? 'New account' : 'Edit account'}</p>
            <h3 id="user-editor-title">{userId === null ? 'Add a user' : 'Update user'}</h3>
          </div>
          <IconButton
            icon="close"
            label="Close user editor"
            onClick={onClose}
            disabled={isSaving}
          />
        </div>

        {loadState === 'loading' && <p className={styles.subtle}>Loading user…</p>}
        {loadState === 'error' && (
          <div className={styles.emptyState}>
            <p>
              {loadError instanceof ApiError && loadError.isUnauthenticated
                ? 'Your session has ended.'
                : loadError instanceof ApiError && loadError.isForbidden
                  ? 'Access denied to this account.'
                  : 'Could not load this account.'}
            </p>
            {loadError instanceof ApiError && loadError.isUnauthenticated ? (
              <Link to={paths.login}>Log in again</Link>
            ) : loadError instanceof ApiError && loadError.isForbidden ? null : (
              <IconButton
                icon="refresh"
                label="Retry loading user"
                onClick={() => {
                  setLoadState('loading')
                  setReloadKey((key) => key + 1)
                }}
              />
            )}
          </div>
        )}
        {loadState === 'ready' && (
          <form className={styles.editorForm} onSubmit={(event) => void save(event)}>
            <label className={styles.field}>
              <span>Username</span>
              <input
                ref={usernameRef}
                required
                maxLength={100}
                autoComplete="username"
                value={values.username}
                disabled={isSaving}
                onChange={(event) => update('username', event.target.value)}
              />
            </label>
            <label className={styles.field}>
              <span>
                Name <span className={styles.subtle}>(optional)</span>
              </span>
              <input
                maxLength={100}
                value={values.name}
                disabled={isSaving}
                onChange={(event) => update('name', event.target.value)}
              />
            </label>
            <label className={styles.field}>
              <span>{userId === null ? 'Password' : 'New password (optional)'}</span>
              <input
                type="password"
                required={userId === null}
                minLength={8}
                autoComplete="new-password"
                value={values.password}
                disabled={isSaving}
                onChange={(event) => update('password', event.target.value)}
              />
            </label>
            <label className={styles.field}>
              <span>Role</span>
              <select
                value={values.role}
                disabled={isSaving}
                onChange={(event) => update('role', event.target.value)}
              >
                {userId !== null && <option value="">Keep current role</option>}
                <option value="EMPLOYEE">Employee</option>
                <option value="ADMIN">Admin</option>
              </select>
            </label>
            {userId !== null && (
              <p className={styles.subtle}>
                Leave password blank and keep the role selection as is to preserve both.
              </p>
            )}
            {formError && (
              <p className={styles.formError} role="alert">
                {formError}
              </p>
            )}
            <div className={styles.actions}>
              <IconButton
                type="submit"
                variant="primary"
                icon="save"
                label={
                  isSaving ? 'Saving user' : userId === null ? 'Create user' : 'Save user changes'
                }
                disabled={isSaving}
              />
              <IconButton
                icon="close"
                label="Cancel editing"
                onClick={onClose}
                disabled={isSaving}
              />
            </div>
          </form>
        )}
      </section>
    </div>
  )
}
