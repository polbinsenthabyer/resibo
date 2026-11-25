import { Outlet, useLocation } from 'react-router-dom'

import { paths } from '@/app/paths'
import { ThemeToggle } from '@/components/ui/ThemeToggle/ThemeToggle'

import styles from './AppLayout.module.scss'

/**
 * The frame every route renders inside: header, theme control, and an outlet for
 * the active route. Routes own their content and nothing else, so the chrome
 * is defined once here.
 */
export function AppLayout() {
  const { pathname } = useLocation()
  const isAdminRoute = pathname === paths.admin || pathname.startsWith(`${paths.admin}/`)

  return (
    <div className={styles.shell}>
      <header className={styles.header}>
        <h1 className={styles.brand}>
          <img className={styles.brandMark} src="/resibo-logo.svg" alt="" aria-hidden="true" />
          Resibo
        </h1>
        <div className={styles.themeControl}>
          <ThemeToggle />
        </div>
      </header>

      <main className={`${styles.main} ${isAdminRoute ? styles.adminMain : ''}`}>
        <Outlet />
      </main>
    </div>
  )
}
