import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'

import { paths } from '@/app/paths'

import { getStoredRole } from '../session'
import type { UserRole } from '../types'

interface RequireRoleProps {
  role: UserRole
  children: ReactNode
}

/** Client navigation gate. The backend checks the JWT on every protected API call. */
export function RequireRole({ role, children }: RequireRoleProps) {
  const storedRole = getStoredRole()

  if (!storedRole) return <Navigate to={paths.login} replace />
  if (storedRole !== role) {
    return <Navigate to={storedRole === 'ADMIN' ? paths.admin : paths.employee} replace />
  }

  return children
}
