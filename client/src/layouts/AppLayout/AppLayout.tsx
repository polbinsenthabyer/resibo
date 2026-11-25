import { useState } from 'react'
import { Outlet, useLocation } from 'react-router-dom'

import { paths } from '@/app/paths'
import { IconButton } from '@/components/ui/IconButton/IconButton'
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
  const [menuOpen, setMenuOpen] = useState(false)

  return (
    <div className={styles.shell}>
      <header className={styles.header} inert={isAdminRoute && menuOpen}>
        <h1 className={styles.brand}>
          <img className={styles.brandMark} src="/resibo-logo.svg" alt="" aria-hidden="true" />
          Resibo
        </h1>
        {isAdminRoute && (
          <div className={styles.adminMenuControl}>
            <IconButton
              id="admin-menu-button"
              icon="menu"
              label={menuOpen ? 'Close admin menu' : 'Open admin menu'}
              aria-expanded={menuOpen}
              aria-controls="admin-mobile-drawer"
              onClick={() => setMenuOpen((open) => !open)}
            />
          </div>
        )}
        <div className={`${styles.themeControl} ${isAdminRoute ? styles.adminThemeControl : ''}`}>
          <ThemeToggle />
        </div>
      </header>

      <main className={`${styles.main} ${isAdminRoute ? styles.adminMain : ''}`}>
        <Outlet context={{ menuOpen, setMenuOpen }} />
      </main>
    </div>
  )
}
