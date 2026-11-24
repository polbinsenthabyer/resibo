import { Navigate } from 'react-router-dom'

import { paths } from '@/app/paths'

import { getStoredRole } from '../session'

export function HomeRedirect() {
  const role = getStoredRole()
  return (
    <Navigate
      to={role === 'ADMIN' ? paths.admin : role === 'EMPLOYEE' ? paths.employee : paths.login}
      replace
    />
  )
}
