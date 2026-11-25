import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'

import { ApiError } from '@/api/errors'
import { userApi } from '@/api/users/api'
import type { ManagedUser, ManagedUserRole } from '@/api/users/types'
import { paths } from '@/app/paths'
import { IconButton } from '@/components/ui/IconButton/IconButton'
import { useAsync } from '@/hooks/useAsync'

import { UserEditor } from '../components/UserEditor'

import styles from './AdminPage.module.scss'

const pageSize = 10
type StatusFilter = 'all' | 'active' | 'inactive'

interface Filters {
  username: string
  role: '' | ManagedUserRole
  status: StatusFilter
}

const emptyFilters: Filters = { username: '', role: '', status: 'all' }

export function AdminUsersPage() {
  const [draft, setDraft] = useState<Filters>(emptyFilters)
  const [filters, setFilters] = useState<Filters>(emptyFilters)
  const [page, setPage] = useState(0)
  const [reloadKey, setReloadKey] = useState(0)
  const [editor, setEditor] = useState<'new' | number | null>(null)
  const editorTrigger = useRef<HTMLButtonElement | null>(null)
  const [changingId, setChangingId] = useState<number | null>(null)

  const { status, data, error, run } = useAsync((signal) =>
    userApi.list(
      {
        ...(filters.username ? { username: filters.username } : {}),
        ...(filters.role ? { role: filters.role } : {}),
        ...(filters.status === 'all' ? {} : { isActive: filters.status === 'active' }),
        page,
        size: pageSize,
        sortBy: 'username',
        sortDirection: 'ASC',
      },
      signal,
    ),
  )
  const accessBlocked =
    status === 'error' &&
    error instanceof ApiError &&
    (error.isUnauthenticated || error.isForbidden || error.httpStatus === 404)
  const hasFilters = Boolean(filters.username || filters.role || filters.status !== 'all')

  useEffect(() => {
    void run()
  }, [filters, page, reloadKey, run])

  function submitFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const next = { ...draft, username: draft.username.trim() }
    setPage(0)
    setFilters(next)
  }

  function closeEditor() {
    setEditor(null)
    window.requestAnimationFrame(() => {
      if (editorTrigger.current?.isConnected) editorTrigger.current.focus()
      else document.getElementById('add-user-button')?.focus()
    })
  }

  async function changeActive(user: ManagedUser) {
    if (
      user.isActive &&
      !window.confirm(`Deactivate "${user.username}"? They will not be able to sign in.`)
    ) {
      return
    }
    setChangingId(user.id)
    try {
      if (user.isActive) await userApi.deactivate(user.id)
      else await userApi.activate(user.id)
      if (filters.status !== 'all' && data?.items.length === 1 && page > 0) {
        setPage((current) => current - 1)
      } else {
        setReloadKey((key) => key + 1)
      }
    } catch {
      // The HTTP layer already publishes the backend error popup.
    } finally {
      setChangingId(null)
    }
  }

  return (
    <section aria-labelledby="users-title" className={styles.page}>
      <header className={styles.pageHeader}>
        <div>
          <p className={styles.eyebrow}>Accounts</p>
          <h2 id="users-title">Users</h2>
          <p className={styles.subtle}>Manage admin and employee access.</p>
        </div>
        <IconButton
          id="add-user-button"
          variant="primary"
          icon="add"
          label="Add user"
          disabled={accessBlocked}
          onClick={(event) => {
            editorTrigger.current = event.currentTarget
            setEditor('new')
          }}
        />
      </header>

      {editor !== null && (
        <UserEditor
          key={editor}
          userId={editor === 'new' ? null : editor}
          onClose={closeEditor}
          onSaved={() => {
            closeEditor()
            setReloadKey((key) => key + 1)
          }}
        />
      )}

      <div className={styles.panel}>
        <form className={styles.userFilterForm} onSubmit={submitFilters}>
          <label className={`${styles.field} ${styles.userSearch}`}>
            <span>Search users</span>
            <input
              maxLength={100}
              placeholder="Username"
              value={draft.username}
              onChange={(event) =>
                setDraft((current) => ({ ...current, username: event.target.value }))
              }
            />
          </label>
          <label className={styles.field}>
            <span>Role</span>
            <select
              value={draft.role}
              onChange={(event) =>
                setDraft((current) => ({ ...current, role: event.target.value as Filters['role'] }))
              }
            >
              <option value="">All roles</option>
              <option value="ADMIN">Admin</option>
              <option value="EMPLOYEE">Employee</option>
            </select>
          </label>
          <label className={styles.field}>
            <span>Status</span>
            <select
              value={draft.status}
              onChange={(event) =>
                setDraft((current) => ({ ...current, status: event.target.value as StatusFilter }))
              }
            >
              <option value="all">All statuses</option>
              <option value="active">Active</option>
              <option value="inactive">Inactive</option>
            </select>
          </label>
          <div className={styles.filterActions}>
            <IconButton type="submit" icon="search" label="Search users" />
            {hasFilters && (
              <IconButton
                icon="close"
                label="Clear user filters"
                onClick={() => {
                  setDraft(emptyFilters)
                  setFilters(emptyFilters)
                  setPage(0)
                }}
              />
            )}
          </div>
        </form>

        {status === 'success' && <span className={styles.count}>{data.totalElements} total</span>}
        {(status === 'idle' || status === 'loading') && (
          <p className={styles.emptyState}>Loading users…</p>
        )}
        {status === 'error' && (
          <div className={styles.emptyState}>
            <p>
              {error instanceof ApiError && error.isUnauthenticated
                ? 'Your session has ended.'
                : error instanceof ApiError && error.isForbidden
                  ? 'Access denied to user management.'
                  : error instanceof ApiError && error.httpStatus === 404
                    ? 'User management is not available on this API yet.'
                    : 'Could not load users.'}
            </p>
            {error instanceof ApiError && error.isUnauthenticated ? (
              <Link to={paths.login}>Log in again</Link>
            ) : error instanceof ApiError && error.isForbidden ? null : (
              <IconButton icon="refresh" label="Retry user list" onClick={() => void run()} />
            )}
          </div>
        )}
        {status === 'success' && data.items.length === 0 && (
          <p className={styles.emptyState}>
            {hasFilters ? 'No users match these filters.' : 'No users yet. Add the first account.'}
          </p>
        )}
        {status === 'success' && data.items.length > 0 && (
          <div className={styles.productList}>
            {data.items.map((user) => (
              <article className={styles.productRow} key={user.id}>
                <div className={styles.productInfo}>
                  <h4>{user.username}</h4>
                  {user.name && <p className={styles.subtle}>{user.name}</p>}
                </div>
                <div className={styles.userMeta}>
                  <span className={styles.subtle}>Role unavailable</span>
                  <span className={user.isActive ? styles.activeStatus : styles.inactiveStatus}>
                    {user.isActive ? 'Active' : 'Inactive'}
                  </span>
                </div>
                <div className={styles.rowActions}>
                  <IconButton
                    icon="edit"
                    label={`Edit ${user.username}`}
                    onClick={(event) => {
                      editorTrigger.current = event.currentTarget
                      setEditor(user.id)
                    }}
                    disabled={changingId !== null}
                  />
                  <IconButton
                    icon={user.isActive ? 'deactivate' : 'activate'}
                    variant={user.isActive ? 'danger' : 'default'}
                    label={`${user.isActive ? 'Deactivate' : 'Activate'} ${user.username}`}
                    onClick={() => void changeActive(user)}
                    disabled={changingId !== null}
                  />
                </div>
              </article>
            ))}
          </div>
        )}
        {status === 'success' && data.totalPages > 1 && (
          <div className={styles.pagination}>
            <IconButton
              icon="previous"
              label="Previous page"
              onClick={() => setPage((current) => current - 1)}
              disabled={!data.hasPrevious}
            />
            <span>
              Page {data.page + 1} of {data.totalPages}
            </span>
            <IconButton
              icon="next"
              label="Next page"
              onClick={() => setPage((current) => current + 1)}
              disabled={!data.hasNext}
            />
          </div>
        )}
      </div>
    </section>
  )
}
