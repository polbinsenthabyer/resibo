import {
  Check,
  ChevronLeft,
  ChevronRight,
  LayoutDashboard,
  LayoutGrid,
  List,
  LogOut,
  Package,
  Pencil,
  Plus,
  RefreshCw,
  Search,
  Trash2,
  X,
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'

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
  | 'rowView'
  | 'cardView'

interface AdminIconProps {
  name: AdminIconName
  className?: string
}

const icons: Record<AdminIconName, LucideIcon> = {
  overview: LayoutDashboard,
  products: Package,
  signout: LogOut,
  add: Plus,
  search: Search,
  close: X,
  edit: Pencil,
  delete: Trash2,
  refresh: RefreshCw,
  previous: ChevronLeft,
  next: ChevronRight,
  save: Check,
  rowView: List,
  cardView: LayoutGrid,
}

/** Shared Lucide symbols. Control labels remain separate and accessible. */
export function AdminIcon({ name, className = '' }: AdminIconProps) {
  const Icon = icons[name]
  return <Icon className={`${styles.icon} ${className}`} aria-hidden="true" strokeWidth={1.9} />
}
