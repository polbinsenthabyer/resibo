export type Theme = 'light' | 'dark'

const THEME_KEY = 'resibo.theme'

export function currentTheme(): Theme {
  return document.documentElement.dataset.theme === 'dark' ? 'dark' : 'light'
}

export function initializeTheme(): void {
  let theme: Theme | null = null
  try {
    const saved = window.localStorage.getItem(THEME_KEY)
    if (saved === 'light' || saved === 'dark') theme = saved
  } catch {
    // Storage may be unavailable in private browsing; the theme still works.
  }

  if (!theme) {
    theme = window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
  }
  document.documentElement.dataset.theme = theme
}

export function setTheme(theme: Theme): void {
  document.documentElement.dataset.theme = theme
  try {
    window.localStorage.setItem(THEME_KEY, theme)
  } catch {
    // Keep the selected theme for this page even when it cannot be persisted.
  }
}
