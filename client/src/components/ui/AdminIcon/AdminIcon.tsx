import styles from './AdminIcon.module.scss'

export type AdminIconName =
  | 'overview'
  | 'products'
  | 'signout'
  | 'add'
  | 'search'
  | 'close'
  | 'edit'
  | 'delete'
  | 'refresh'
  | 'previous'
  | 'next'
  | 'save'

interface AdminIconProps {
  name: AdminIconName
  className?: string
}

/** Magnific-generated icons from the `pol` project. Text labels live on controls. */
export function AdminIcon({ name, className = '' }: AdminIconProps) {
  return (
    <img
      className={`${styles.icon} ${name === 'next' ? styles.next : ''} ${className}`}
      src={`/icons/admin-${name === 'next' ? 'previous' : name}.svg`}
      alt=""
      aria-hidden="true"
    />
  )
}
