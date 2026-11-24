import { useNavigate } from 'react-router-dom'

import { paths } from '@/app/paths'
import { Button } from '@/components/ui/Button'

import { authApi } from '../api'
import type { UserRole } from '../types'

import styles from './AuthPage.module.scss'

interface RoleWelcomePageProps {
  role: UserRole
}

export function RoleWelcomePage({ role }: RoleWelcomePageProps) {
  const navigate = useNavigate()

  function handleSignOut() {
    authApi.logout()
    navigate(paths.login, { replace: true })
  }

  return (
    <section className={styles.page}>
      <h2>Welcome {role === 'ADMIN' ? 'Admin' : 'Employee'} to RESIBO POS</h2>
      <p className={styles.intro}>Your {role.toLowerCase()} workspace will go here.</p>
      <div className={`${styles.actions} ${styles.welcomeActions}`}>
        <Button onClick={handleSignOut}>Sign out</Button>
      </div>
    </section>
  )
}
