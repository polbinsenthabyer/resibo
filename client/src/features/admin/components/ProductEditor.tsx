import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'

import { ApiError } from '@/api/errors'
import { productApi } from '@/api/products/api'
import type { ProductUpsertRequest } from '@/api/products/types'
import { paths } from '@/app/paths'
import { IconButton } from '@/components/ui/IconButton/IconButton'
import styles from '../pages/AdminPage.module.scss'

interface ProductEditorProps {
  productId: number | null
  onClose: () => void
  onSaved: () => void
}

interface FormValues {
  name: string
  description: string
  price: string
  stock: string
}

const emptyForm: FormValues = { name: '', description: '', price: '0.00', stock: '0' }

export function ProductEditor({ productId, onClose, onSaved }: ProductEditorProps) {
  const dialogRef = useRef<HTMLElement>(null)
  const nameInputRef = useRef<HTMLInputElement>(null)
  const [values, setValues] = useState<FormValues>(emptyForm)
  const [loadState, setLoadState] = useState<'ready' | 'loading' | 'error'>(
    productId === null ? 'ready' : 'loading',
  )
  const [reloadKey, setReloadKey] = useState(0)
  const [isSaving, setIsSaving] = useState(false)
  const [loadError, setLoadError] = useState<Error | null>(null)
  const [formError, setFormError] = useState<string | null>(null)

  useEffect(() => {
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    dialogRef.current?.focus()
    return () => {
      document.body.style.overflow = previousOverflow
    }
  }, [])

  useEffect(() => {
    if (loadState === 'ready') nameInputRef.current?.focus()
  }, [loadState])

  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      const dialog = dialogRef.current
      if (!dialog) return
      if (event.key === 'Escape') {
        event.preventDefault()
        if (!isSaving) onClose()
        return
      }
      if (event.key !== 'Tab') return

      const controls = Array.from(
        dialog.querySelectorAll<HTMLElement>(
          'a[href], button:not([disabled]), input:not([disabled]), textarea:not([disabled])',
        ),
      )
      const first = controls[0]
      const last = controls.at(-1)
      if (!first || !last) {
        event.preventDefault()
        dialog.focus()
      } else if (
        event.shiftKey &&
        (document.activeElement === first ||
          document.activeElement === dialog ||
          !dialog.contains(document.activeElement))
      ) {
        event.preventDefault()
        last.focus()
      } else if (
        !event.shiftKey &&
        (document.activeElement === last ||
          document.activeElement === dialog ||
          !dialog.contains(document.activeElement))
      ) {
        event.preventDefault()
        first.focus()
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [isSaving, onClose])

  useEffect(() => {
    if (productId === null) return
    const controller = new AbortController()
    productApi.getById(productId, controller.signal).then(
      (product) => {
        if (controller.signal.aborted) return
        setValues({
          name: product.productName,
          description: product.productDescription ?? '',
          price: product.productPrice.toFixed(2),
          stock: String(product.productRemainingStocks),
        })
        setLoadState('ready')
      },
      (cause) => {
        if (!controller.signal.aborted) {
          setLoadError(cause instanceof Error ? cause : new Error('Could not load product.'))
          setLoadState('error')
        }
      },
    )
    return () => controller.abort()
  }, [productId, reloadKey])

  function update(field: keyof FormValues, value: string) {
    setFormError(null)
    setValues((current) => ({ ...current, [field]: value }))
  }

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (isSaving) return

    const name = values.name.trim()
    const description = values.description.trim()
    if (!name || name.length > 100) {
      setFormError('Enter a product name of at most 100 characters.')
      return
    }
    if (description.length > 250) {
      setFormError('Keep the description within 250 characters.')
      return
    }
    if (!/^\d+(\.\d{1,2})?$/.test(values.price) || Number(values.price) > 99999999.99) {
      setFormError('Enter a nonnegative price with at most two decimal places.')
      return
    }
    if (!/^\d+$/.test(values.stock) || Number(values.stock) > 2147483647) {
      setFormError('Enter a nonnegative whole-number stock quantity.')
      return
    }

    const payload: ProductUpsertRequest = {
      ...(productId === null ? {} : { id: productId }),
      productName: name,
      productDescription: description,
      productPrice: Number(values.price),
      productRemainingStocks: Number(values.stock),
    }
    setFormError(null)
    setIsSaving(true)
    try {
      await productApi.save(payload)
      onSaved()
    } catch (cause) {
      setFormError(cause instanceof Error ? cause.message : 'Could not save product.')
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <div className={styles.editorOverlay}>
      <section
        ref={dialogRef}
        className={styles.editor}
        role="dialog"
        aria-modal="true"
        aria-labelledby="product-editor-title"
        tabIndex={-1}
      >
        <div className={styles.sectionHeading}>
          <div>
            <p className={styles.eyebrow}>{productId === null ? 'New product' : 'Edit product'}</p>
            <h3 id="product-editor-title">
              {productId === null ? 'Add a product' : 'Update product'}
            </h3>
          </div>
          <IconButton
            icon="close"
            label="Close product editor"
            onClick={onClose}
            disabled={isSaving}
          />
        </div>

        {loadState === 'loading' && <p className={styles.subtle}>Loading product…</p>}
        {loadState === 'error' && (
          <div className={styles.emptyState}>
            <p>
              {loadError instanceof ApiError && loadError.isUnauthenticated
                ? 'Your session has ended.'
                : loadError instanceof ApiError && loadError.isForbidden
                  ? 'Access denied to this product.'
                  : 'Could not load this product.'}
            </p>
            {loadError instanceof ApiError && loadError.isUnauthenticated ? (
              <Link to={paths.login}>Log in again</Link>
            ) : loadError instanceof ApiError && loadError.isForbidden ? null : (
              <IconButton
                icon="refresh"
                label="Retry loading product"
                onClick={() => {
                  setLoadState('loading')
                  setReloadKey((key) => key + 1)
                }}
              />
            )}
          </div>
        )}
        {loadState === 'ready' && (
          <form className={styles.editorForm} onSubmit={(event) => void save(event)}>
            <label className={styles.field}>
              <span>Product name</span>
              <input
                ref={nameInputRef}
                required
                maxLength={100}
                value={values.name}
                disabled={isSaving}
                onChange={(event) => update('name', event.target.value)}
              />
            </label>
            <label className={styles.field}>
              <span>
                Description <span className={styles.subtle}>(optional)</span>
              </span>
              <textarea
                maxLength={250}
                rows={3}
                value={values.description}
                disabled={isSaving}
                onChange={(event) => update('description', event.target.value)}
              />
            </label>
            <div className={styles.fieldPair}>
              <label className={styles.field}>
                <span>Price</span>
                <input
                  required
                  inputMode="decimal"
                  value={values.price}
                  disabled={isSaving}
                  onChange={(event) => update('price', event.target.value)}
                />
              </label>
              <label className={styles.field}>
                <span>Stock</span>
                <input
                  required
                  inputMode="numeric"
                  value={values.stock}
                  disabled={isSaving}
                  onChange={(event) => update('stock', event.target.value)}
                />
              </label>
            </div>
            <p className={styles.subtle}>
              Price has up to two decimal places. Stock is a whole number.
            </p>
            {formError && (
              <p className={styles.formError} role="alert">
                {formError}
              </p>
            )}
            <div className={styles.actions}>
              <IconButton
                type="submit"
                variant="primary"
                icon="save"
                label={
                  isSaving
                    ? 'Saving product'
                    : productId === null
                      ? 'Create product'
                      : 'Save product changes'
                }
                disabled={isSaving}
              />
              <IconButton
                icon="close"
                label="Cancel editing"
                onClick={onClose}
                disabled={isSaving}
              />
            </div>
          </form>
        )}
      </section>
    </div>
  )
}
