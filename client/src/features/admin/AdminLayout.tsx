import { NavLink, Outlet, useNavigate } from 'react-router-dom'

import { paths } from '@/app/paths'
import { tokenStorage } from '@/api/tokenStorage'
import { AdminIcon } from '@/components/ui/AdminIcon/AdminIcon'
import { IconButton } from '@/components/ui/IconButton/IconButton'

import styles from './AdminLayout.module.scss'

export function AdminLayout() {
  const navigate = useNavigate()

  function signOut() {
    tokenStorage.clear()
    navigate(paths.login, { replace: true })
  }

  return (
    <div className={styles.workspace}>
      <aside className={styles.sidebar} aria-label="Admin navigation">
        <div className={styles.sidebarTop}>
          <div>
            <p className={styles.eyebrow}>Workspace</p>
            <p className={styles.sidebarTitle}>Admin</p>
          </div>
        </div>
        <div className={styles.menuContent}>
          <nav className={styles.nav} aria-label="Admin sections">
            <NavLink
              className={({ isActive }) => `${styles.navLink} ${isActive ? styles.active : ''}`}
              end
              to={paths.admin}
            >
              <AdminIcon name="overview" />
              Overview
            </NavLink>
            <NavLink
              className={({ isActive }) => `${styles.navLink} ${isActive ? styles.active : ''}`}
              to={paths.adminProducts}
            >
              <AdminIcon name="products" />
              Products
            </NavLink>
          </nav>
          <IconButton
            className={styles.signOut}
            icon="signout"
            label="Sign out"
            onClick={signOut}
          />
        </div>
      </aside>
      <div className={styles.content}>
        <Outlet />
      </div>
    </div>
  )
}
