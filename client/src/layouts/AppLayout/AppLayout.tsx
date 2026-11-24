import { Outlet } from 'react-router-dom'

import { ThemeToggle } from '@/components/ui/ThemeToggle/ThemeToggle'

import styles from './AppLayout.module.scss'

/**
 * The frame every route renders inside: header, theme control, and an outlet for
 * the active route. Routes own their content and nothing else, so the chrome
 * is defined once here.
 */
export function AppLayout() {
  return (
    <div className={styles.shell}>
      <header className={styles.header}>
        <h1 className={styles.brand}>Resibo</h1>
        <span className={styles.tagline}>point of sale</span>
        <div className={styles.themeControl}>
          <ThemeToggle />
        </div>
      </header>

      <main className={styles.main}>
        <Outlet />
      </main>
    </div>
  )
}
