import { createBrowserRouter } from 'react-router-dom'

import { AdminLayout } from '@/features/admin/AdminLayout'
import { AdminDashboardPage } from '@/features/admin/pages/AdminDashboardPage'
import { AdminProductsPage } from '@/features/admin/pages/AdminProductsPage'
import { AdminUsersPage } from '@/features/admin/pages/AdminUsersPage'
import { HomeRedirect } from '@/features/auth/components/HomeRedirect'
import { RequireRole } from '@/features/auth/components/RequireRole'
import { LoginPage } from '@/features/auth/pages/LoginPage'
import { RoleWelcomePage } from '@/features/auth/pages/RoleWelcomePage'
import { SignupPage } from '@/features/auth/pages/SignupPage'
import { HealthPage } from '@/features/health/pages/HealthPage'
import { AppLayout } from '@/layouts/AppLayout'
import { NotFoundPage } from '@/pages/NotFoundPage'

import { paths } from './paths'

/**
 * The route table. One place to see every screen the app has.
 *
 * Routes are declared statically for now. When a screen becomes heavy enough
 * to be worth splitting, give it a `lazy` entry here rather than importing it
 * eagerly — that keeps the decision visible in this file instead of buried in
 * the page.
 */
export const router = createBrowserRouter([
  {
    path: paths.home,
    element: <AppLayout />,
    children: [
      { index: true, element: <HomeRedirect /> },
      { path: paths.status, element: <HealthPage /> },
      { path: paths.login, element: <LoginPage /> },
      { path: paths.signup, element: <SignupPage /> },
      {
        path: paths.admin,
        element: (
          <RequireRole role="ADMIN">
            <AdminLayout />
          </RequireRole>
        ),
        children: [
          { index: true, element: <AdminDashboardPage /> },
          { path: 'products', element: <AdminProductsPage /> },
          { path: 'users', element: <AdminUsersPage /> },
        ],
      },
      {
        path: paths.employee,
        element: (
          <RequireRole role="EMPLOYEE">
            <RoleWelcomePage role="EMPLOYEE" />
          </RequireRole>
        ),
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
])
