import { RouterProvider } from 'react-router-dom'

import { router } from '@/app/router'

/**
 * Application root. The place where app-wide providers are composed —
 * router today, and whatever state or theme context arrives later. Keeping it
 * free of markup means a new provider is a one-line change here rather than a
 * refactor of a page.
 */
export function App() {
  return <RouterProvider router={router} />
}
