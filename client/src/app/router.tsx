import { createBrowserRouter } from 'react-router-dom'

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
      { index: true, element: <HealthPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
])
