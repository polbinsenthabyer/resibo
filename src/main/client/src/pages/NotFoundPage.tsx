import { Link } from 'react-router-dom'

import { paths } from '@/app/paths'

/**
 * Lives in `pages/` rather than under a feature, because it belongs to no
 * feature. Anything in here should be genuinely cross-cutting; if a page has
 * a feature, it goes in that feature's `pages/` folder instead.
 */
export function NotFoundPage() {
  return (
    <section>
      <h2>Page not found</h2>
      <p>That route does not exist.</p>
      <Link to={paths.home}>Back to status</Link>
    </section>
  )
}
