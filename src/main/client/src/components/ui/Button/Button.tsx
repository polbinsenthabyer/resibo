import type { ButtonHTMLAttributes, ReactNode } from 'react'

import { cx } from '@/lib/classNames'

import styles from './Button.module.scss'

export type ButtonVariant = 'default' | 'primary' | 'danger'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant
  children: ReactNode
}

/**
 * The project's button.
 *
 * `type` defaults to "button" rather than the HTML default of "submit",
 * because an unlabelled button inside a form submitting it by accident is a
 * bug nobody looks for.
 */
export function Button({ variant = 'default', className, children, ...rest }: ButtonProps) {
  return (
    <button
      type="button"
      className={cx(
        styles.button,
        variant === 'primary' && styles.primary,
        variant === 'danger' && styles.danger,
        className,
      )}
      {...rest}
    >
      {children}
    </button>
  )
}
