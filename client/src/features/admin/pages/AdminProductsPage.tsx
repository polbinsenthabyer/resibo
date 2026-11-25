import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'

import { ApiError } from '@/api/errors'
import { productApi } from '@/api/products/api'
import type { Product } from '@/api/products/types'
import { paths } from '@/app/paths'
import { IconButton } from '@/components/ui/IconButton/IconButton'
import { useAsync } from '@/hooks/useAsync'

import { ProductEditor } from '../components/ProductEditor'

import styles from './AdminPage.module.scss'

const pageSize = 10

export function AdminProductsPage() {
  const [searchInput, setSearchInput] = useState('')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [reloadKey, setReloadKey] = useState(0)
  const [editor, setEditor] = useState<'new' | number | null>(null)
  const editorTrigger = useRef<HTMLButtonElement | null>(null)
  const [deletingId, setDeletingId] = useState<number | null>(null)
  const { status, data, error, run } = useAsync((signal) =>
    productApi.list(
      {
        ...(search ? { productName: search } : {}),
        page,
        size: pageSize,
        sortBy: 'productName',
        sortDirection: 'ASC',
      },
      signal,
    ),
  )
  const accessBlocked =
    status === 'error' &&
    error instanceof ApiError &&
    (error.isUnauthenticated || error.isForbidden)

  useEffect(() => {
    void run()
  }, [search, page, reloadKey, run])

  function submitSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const next = searchInput.trim()
    setPage(0)
    setSearch(next)
    if (next === search && page === 0) setReloadKey((key) => key + 1)
  }

  function closeEditor() {
    setEditor(null)
    window.requestAnimationFrame(() => {
      if (editorTrigger.current?.isConnected) editorTrigger.current.focus()
      else document.getElementById('add-product-button')?.focus()
    })
  }

  async function remove(product: Product) {
    if (!window.confirm(`Delete "${product.productName}"? This cannot be undone.`)) return
    setDeletingId(product.id)
    try {
      await productApi.remove(product.id)
      if (data?.items.length === 1 && page > 0) setPage((current) => current - 1)
      else setReloadKey((key) => key + 1)
    } catch {
      // The shared HTTP client already shows the backend error popup.
    } finally {
      setDeletingId(null)
    }
  }

  return (
    <section aria-labelledby="products-title" className={styles.page}>
      <header className={styles.pageHeader}>
        <div>
          <p className={styles.eyebrow}>Catalog</p>
          <h2 id="products-title">Products</h2>
          <p className={styles.subtle}>Manage the items available in Resibo.</p>
        </div>
        <IconButton
          id="add-product-button"
          variant="primary"
          icon="add"
          label="Add product"
          disabled={accessBlocked}
          onClick={(event) => {
            editorTrigger.current = event.currentTarget
            setEditor('new')
          }}
        />
      </header>

      {editor !== null && (
        <ProductEditor
          key={editor}
          productId={editor === 'new' ? null : editor}
          onClose={closeEditor}
          onSaved={() => {
            closeEditor()
            setReloadKey((key) => key + 1)
          }}
        />
      )}

      <div className={styles.panel}>
        <div className={styles.sectionHeading}>
          <h3>Product list</h3>
          {status === 'success' && <span className={styles.count}>{data.totalElements} total</span>}
        </div>
        <form className={styles.searchForm} onSubmit={submitSearch}>
          <label className={styles.searchLabel} htmlFor="product-search">
            Search products
          </label>
          <div className={styles.searchControls}>
            <input
              id="product-search"
              maxLength={100}
              placeholder="Product name"
              value={searchInput}
              onChange={(event) => setSearchInput(event.target.value)}
            />
            <IconButton type="submit" icon="search" label="Search products" />
            {search && (
              <IconButton
                icon="close"
                label="Clear search"
                onClick={() => {
                  setSearchInput('')
                  setSearch('')
                  setPage(0)
                }}
              />
            )}
          </div>
        </form>

        {(status === 'idle' || status === 'loading') && (
          <p className={styles.emptyState}>Loading products…</p>
        )}
        {status === 'error' && (
          <div className={styles.emptyState}>
            <p>
              {error instanceof ApiError && error.isUnauthenticated
                ? 'Your session has ended.'
                : error instanceof ApiError && error.isForbidden
                  ? 'Access denied to the product catalog.'
                  : 'Could not load products.'}
            </p>
            {error instanceof ApiError && error.isUnauthenticated ? (
              <Link to={paths.login}>Log in again</Link>
            ) : error instanceof ApiError && error.isForbidden ? null : (
              <IconButton icon="refresh" label="Retry product list" onClick={() => void run()} />
            )}
          </div>
        )}
        {status === 'success' && data.items.length === 0 && (
          <p className={styles.emptyState}>
            {search ? 'No products match that search.' : 'No products yet. Add your first item.'}
          </p>
        )}
        {status === 'success' && data.items.length > 0 && (
          <div className={styles.productList}>
            {data.items.map((product) => (
              <article className={styles.productRow} key={product.id}>
                <div className={styles.productInfo}>
                  <h4>{product.productName}</h4>
                  {product.productDescription && (
                    <p className={styles.subtle}>{product.productDescription}</p>
                  )}
                </div>
                <div className={styles.productMeta}>
                  <span>
                    Price <strong>{product.productPrice.toFixed(2)}</strong>
                  </span>
                  <span className={product.productRemainingStocks === 0 ? styles.outOfStock : ''}>
                    {product.productRemainingStocks === 0
                      ? 'Out of stock'
                      : `${product.productRemainingStocks} in stock`}
                  </span>
                </div>
                <div className={styles.rowActions}>
                  <IconButton
                    icon="edit"
                    label={`Edit ${product.productName}`}
                    onClick={(event) => {
                      editorTrigger.current = event.currentTarget
                      setEditor(product.id)
                    }}
                    disabled={deletingId !== null}
                  />
                  <IconButton
                    variant="danger"
                    icon="delete"
                    label={
                      deletingId === product.id
                        ? `Deleting ${product.productName}`
                        : `Delete ${product.productName}`
                    }
                    onClick={() => void remove(product)}
                    disabled={deletingId !== null}
                  />
                </div>
              </article>
            ))}
          </div>
        )}
        {status === 'success' && data.totalPages > 1 && (
          <div className={styles.pagination}>
            <IconButton
              icon="previous"
              label="Previous page"
              onClick={() => setPage((current) => current - 1)}
              disabled={!data.hasPrevious}
            />
            <span>
              Page {data.page + 1} of {data.totalPages}
            </span>
            <IconButton
              icon="next"
              label="Next page"
              onClick={() => setPage((current) => current + 1)}
              disabled={!data.hasNext}
            />
          </div>
        )}
      </div>
    </section>
  )
}
