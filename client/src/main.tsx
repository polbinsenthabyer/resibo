import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'

import { App } from './App'
import { initializeTheme } from './theme'

// The only global stylesheet: tokens, reset and element defaults. Everything
// else is a component-scoped *.module.scss.
import '@/styles/main.scss'

const container = document.getElementById('root')
if (!container) {
  // index.html owns this element. If it is missing, the HTML and this entry
  // point have diverged, and failing loudly beats a blank page.
  throw new Error('Root element #root was not found in index.html.')
}

initializeTheme()

createRoot(container).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
