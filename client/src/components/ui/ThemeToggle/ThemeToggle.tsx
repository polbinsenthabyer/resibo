import { useState } from 'react'

import { currentTheme, setTheme } from '@/theme'
import type { Theme } from '@/theme'

import { Button } from '../Button'

export function ThemeToggle() {
  const [theme, updateTheme] = useState<Theme>(() => currentTheme())

  function toggleTheme() {
    const nextTheme = theme === 'dark' ? 'light' : 'dark'
    setTheme(nextTheme)
    updateTheme(nextTheme)
  }

  return (
    <Button
      onClick={toggleTheme}
      aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}
    >
      {theme === 'dark' ? 'Light mode' : 'Dark mode'}
    </Button>
  )
}
