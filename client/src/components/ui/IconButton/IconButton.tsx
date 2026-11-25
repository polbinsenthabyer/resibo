import type { ButtonHTMLAttributes } from 'react'

import { Button } from '@/components/ui/Button'
import type { ButtonVariant } from '@/components/ui/Button/Button'
import { AdminIcon } from '@/components/ui/AdminIcon/AdminIcon'
import type { AdminIconName } from '@/components/ui/AdminIcon/AdminIcon'

import styles from './IconButton.module.scss'

interface IconButtonProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'> {
  icon: AdminIconName
  label: string
  variant?: ButtonVariant
}

export function IconButton({
  icon,
  label,
  variant = 'default',
  className = '',
  ...rest
}: IconButtonProps) {
  return (
    <Button
      {...rest}
      variant={variant}
      className={`${styles.iconButton} ${variant !== 'default' ? styles.onFill : ''} ${className}`}
      aria-label={label}
      title={label}
    >
      <AdminIcon name={icon} />
    </Button>
  )
}
