import { Link } from 'react-router-dom'

import { ApiError } from '@/api/errors'
import { paths } from '@/app/paths'
import { productApi } from '@/api/products/api'
import { AdminIcon } from '@/components/ui/AdminIcon/AdminIcon'
import { IconButton } from '@/components/ui/IconButton/IconButton'
import { useAsync } from '@/hooks/useAsync'

import styles from './AdminPage.module.scss'

export function AdminDashboardPage() {
  const catalog = useAsync((signal) => productApi.list({ page: 0, size: 1 }, signal), {
    immediate: true,
  })

  return (
    <section aria-labelledby="admin-title" className={styles.page}>
      <header className={styles.pageHeader}>
        <div>
          <p className={styles.eyebrow}>Overview</p>
          <h2 id="admin-title">Welcome, Admin</h2>
          <p className={styles.subtle}>Your product catalog is ready to manage.</p>
        </div>
      </header>

      <div className={styles.dashboardGrid}>
        <div className={styles.panel}>
          <p className={styles.subtle}>Total products</p>
          {catalog.status === 'success' && (
            <p className={styles.metric}>{catalog.data.totalElements}</p>
          )}
          {catalog.status === 'loading' && <p className={styles.metricPlaceholder}>Loading…</p>}
          {catalog.status === 'error' && (
            <div>
              {catalog.error instanceof ApiError && catalog.error.isUnauthenticated ? (
                <>
                  <p>Your session has ended.</p>
                  <Link to={paths.login}>Log in again</Link>
                </>
              ) : catalog.error instanceof ApiError && catalog.error.isForbidden ? (
                <p>Access denied to the product catalog.</p>
              ) : (
                <>
                  <p>Catalog unavailable.</p>
                  <IconButton
                    icon="refresh"
                    label="Retry catalog"
                    onClick={() => void catalog.run()}
                  />
                </>
              )}
            </div>
          )}
        </div>
        <div className={styles.panel}>
          <h3>Products</h3>
          <p className={styles.subtle}>Add products, update prices and stock, or remove an item.</p>
          <Link className={styles.actionLink} to={paths.adminProducts}>
            <AdminIcon name="products" />
            Manage products
          </Link>
        </div>
      </div>
    </section>
  )
}
