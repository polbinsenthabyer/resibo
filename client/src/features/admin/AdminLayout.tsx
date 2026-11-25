import { useEffect } from 'react'
import type { Dispatch, SetStateAction } from 'react'
import { NavLink, Outlet, useNavigate, useOutletContext } from 'react-router-dom'

import { paths } from '@/app/paths'
import { tokenStorage } from '@/api/tokenStorage'
import { AdminIcon } from '@/components/ui/AdminIcon/AdminIcon'
import { IconButton } from '@/components/ui/IconButton/IconButton'
import { ThemeToggle } from '@/components/ui/ThemeToggle/ThemeToggle'

import styles from './AdminLayout.module.scss'

interface AdminMenuProps {
  onNavigate?: () => void
  onSignOut: () => void
  showTheme?: boolean
}

function AdminMenu({ onNavigate, onSignOut, showTheme = false }: AdminMenuProps) {
  return (
    <div className={styles.menuContent}>
      <nav className={styles.nav} aria-label="Admin sections">
        <NavLink
          className={({ isActive }) => `${styles.navLink} ${isActive ? styles.active : ''}`}
          end
          to={paths.admin}
          onClick={onNavigate}
        >
          <AdminIcon name="overview" />
          Overview
        </NavLink>
        <NavLink
          className={({ isActive }) => `${styles.navLink} ${isActive ? styles.active : ''}`}
          to={paths.adminProducts}
          onClick={onNavigate}
        >
          <AdminIcon name="products" />
          Products
        </NavLink>
        <NavLink
          className={({ isActive }) => `${styles.navLink} ${isActive ? styles.active : ''}`}
          to={paths.adminUsers}
          onClick={onNavigate}
        >
          <AdminIcon name="users" />
          Users
        </NavLink>
      </nav>
      <div className={styles.footerActions}>
        {showTheme && <ThemeToggle />}
        <IconButton icon="signout" label="Sign out" onClick={onSignOut} />
      </div>
    </div>
  )
}

export function AdminLayout() {
  const navigate = useNavigate()
  const { menuOpen, setMenuOpen } = useOutletContext<{
    menuOpen: boolean
    setMenuOpen: Dispatch<SetStateAction<boolean>>
  }>()

  useEffect(() => {
    if (!menuOpen) return
    const drawer = document.getElementById('admin-mobile-drawer')
    const previousOverflow = document.body.style.overflow
    document.body.style.setProperty('overflow', 'hidden')
    document.getElementById('admin-drawer-close')?.focus()

    function handleKeyDown(event: KeyboardEvent) {
      if (!drawer) return
      if (event.key === 'Escape') {
        event.preventDefault()
        setMenuOpen(false)
        window.requestAnimationFrame(() => document.getElementById('admin-menu-button')?.focus())
        return
      }
      if (event.key !== 'Tab') return
      const controls = Array.from(
        drawer.querySelectorAll<HTMLElement>('a[href], button:not([disabled])'),
      )
      const first = controls[0]
      const last = controls.at(-1)
      if (!first || !last) return
      if (
        event.shiftKey &&
        (document.activeElement === first || !drawer.contains(document.activeElement))
      ) {
        event.preventDefault()
        last.focus()
      } else if (
        !event.shiftKey &&
        (document.activeElement === last || !drawer.contains(document.activeElement))
      ) {
        event.preventDefault()
        first.focus()
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => {
      if (previousOverflow) document.body.style.setProperty('overflow', previousOverflow)
      else document.body.style.removeProperty('overflow')
      document.removeEventListener('keydown', handleKeyDown)
    }
  }, [menuOpen, setMenuOpen])

  useEffect(() => {
    const mobile = window.matchMedia('(max-width: 40rem)')
    const handleResize = () => {
      if (!mobile.matches) setMenuOpen(false)
    }
    mobile.addEventListener('change', handleResize)
    return () => mobile.removeEventListener('change', handleResize)
  }, [setMenuOpen])

  function closeMenu(restoreFocus = true) {
    setMenuOpen(false)
    if (restoreFocus) {
      window.requestAnimationFrame(() => document.getElementById('admin-menu-button')?.focus())
    }
  }

  function signOut() {
    setMenuOpen(false)
    tokenStorage.clear()
    navigate(paths.login, { replace: true })
  }

  return (
    <div className={styles.workspace}>
      <aside className={styles.sidebar} aria-label="Admin navigation" inert={menuOpen}>
        <div className={styles.sidebarTop}>
          <p className={styles.eyebrow}>Workspace</p>
          <p className={styles.sidebarTitle}>Admin</p>
        </div>
        <AdminMenu onSignOut={signOut} />
      </aside>

      <div
        className={`${styles.mobileBackdrop} ${menuOpen ? styles.mobileBackdropOpen : ''}`}
        aria-hidden="true"
        onClick={() => closeMenu()}
      />
      <aside
        id="admin-mobile-drawer"
        className={`${styles.mobileDrawer} ${menuOpen ? styles.mobileDrawerOpen : ''}`}
        role="dialog"
        aria-modal={menuOpen}
        aria-label="Admin menu"
        aria-hidden={!menuOpen}
        inert={!menuOpen}
      >
        <div className={styles.drawerTop}>
          <IconButton
            id="admin-drawer-close"
            icon="close"
            label="Close admin menu"
            onClick={() => closeMenu()}
          />
        </div>
        <AdminMenu onNavigate={() => closeMenu()} onSignOut={signOut} showTheme />
      </aside>

      <div className={styles.content} inert={menuOpen}>
        <Outlet />
      </div>
    </div>
  )
}
