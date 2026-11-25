import { useCallback, useEffect, useRef, useState } from 'react'

import { subscribeToNotifications } from '@/lib/notifications'
import type { NotificationEvent } from '@/lib/notifications'

import styles from './NotificationCenter.module.scss'

interface Notice extends NotificationEvent {
  id: number
}

interface NoticeItemProps {
  notice: Notice
  onDismiss: (id: number) => void
}

function NoticeItem({ notice, onDismiss }: NoticeItemProps) {
  useEffect(() => {
    if (notice.tone !== 'success') return
    const timer = window.setTimeout(() => onDismiss(notice.id), 6000)
    return () => window.clearTimeout(timer)
  }, [notice.id, notice.tone, onDismiss])

  return (
    <div
      className={`${styles.notice} ${notice.tone === 'error' ? styles.error : styles.success}`}
      role={notice.tone === 'error' ? 'alert' : 'status'}
    >
      <div className={styles.content}>
        <strong>{notice.tone === 'error' ? 'Error' : 'Success'}</strong>
        <p>{notice.message}</p>
      </div>
      <button className={styles.dismiss} type="button" onClick={() => onDismiss(notice.id)}>
        Dismiss
      </button>
    </div>
  )
}

/** One popup surface for backend feedback across routes. */
export function NotificationCenter() {
  const [notices, setNotices] = useState<Notice[]>([])
  const nextId = useRef(0)

  const dismiss = useCallback((id: number) => {
    setNotices((current) => current.filter((notice) => notice.id !== id))
  }, [])

  useEffect(
    () =>
      subscribeToNotifications((event) => {
        // Bound the stack so a burst of API failures cannot cover the page.
        setNotices((current) => [...current.slice(-2), { ...event, id: ++nextId.current }])
      }),
    [],
  )

  return (
    <aside className={styles.stack} aria-label="Notifications">
      {notices.map((notice) => (
        <NoticeItem key={notice.id} notice={notice} onDismiss={dismiss} />
      ))}
    </aside>
  )
}
