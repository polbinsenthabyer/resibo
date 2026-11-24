import { NavLink, Outlet } from 'react-router-dom'

import { paths } from '@/app/paths'
import { cx } from '@/lib/classNames'

import styles from './AppLayout.module.scss'

/**
 * The frame every route renders inside: header, navigation, and an outlet for
 * the active route. Routes own their content and nothing else, so the chrome
 * is defined once here.
 */
export function AppLayout() {
  return (
    <div className={styles.shell}>
      <header className={styles.header}>
        <h1 className={styles.brand}>Resibo</h1>
        <span className={styles.tagline}>point of sale</span>
        <nav className={styles.nav} aria-label="Main">
          <NavLink
            to={paths.home}
            className={({ isActive }) => cx(isActive && styles.navLinkActive)}
          >
            Status
          </NavLink>
          <NavLink
            to={paths.login}
            className={({ isActive }) => cx(isActive && styles.navLinkActive)}
          >
            Log in
          </NavLink>
          <NavLink
            to={paths.signup}
            className={({ isActive }) => cx(isActive && styles.navLinkActive)}
          >
            Sign up
          </NavLink>
        </nav>
      </header>

      <main className={styles.main}>
        <Outlet />
      </main>
    </div>
  )
}
