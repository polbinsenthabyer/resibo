import { useState } from 'react'

import { currentTheme, setTheme } from '@/theme'
import type { Theme } from '@/theme'

import { Button } from '../Button'

import styles from './ThemeToggle.module.scss'

export function ThemeToggle() {
  const [theme, updateTheme] = useState<Theme>(() => currentTheme())

  function toggleTheme() {
    const nextTheme = theme === 'dark' ? 'light' : 'dark'
    setTheme(nextTheme)
    updateTheme(nextTheme)
  }

  return (
    <Button
      className={styles.toggle}
      onClick={toggleTheme}
      aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}
      title={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}
    >
      <img
        className={styles.icon}
        src={theme === 'dark' ? '/icons/theme-sun.svg' : '/icons/theme-moon.svg'}
        alt=""
        aria-hidden="true"
      />
    </Button>
  )
}
