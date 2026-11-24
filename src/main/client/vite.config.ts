import { fileURLToPath, URL } from 'node:url'

import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],

  resolve: {
    // Mirrors the "@/*" path mapping in tsconfig.app.json. Both have to agree:
    // TypeScript resolves types with one, Vite resolves the import with the
    // other, and a mismatch type-checks fine and then fails at build time.
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },

  css: {
    // Class names reach TypeScript camelCased (styles.productRow) while the
    // SCSS keeps the CSS convention (.product-row). Without this, every
    // kebab-case class has to be read as styles['product-row'].
    modules: {
      localsConvention: 'camelCaseOnly',
    },
    preprocessorOptions: {
      scss: {
        // Lets any stylesheet say `@use 'abstracts'` without a relative path.
        loadPaths: [fileURLToPath(new URL('./src/styles', import.meta.url))],
        // Design tokens and mixins are injected into every stylesheet, so a
        // component's .module.scss can use them with no boilerplate.
        //
        // IMPORTANT: because this is prepended automatically, never write
        // `@use 'abstracts' as *` in a stylesheet yourself — Sass rejects the
        // same module being loaded twice in one file.
        additionalData: "@use 'abstracts' as *;\n",
      },
    },
  },

  server: {
    port: 5173,
    // Dev requests to /api are proxied to the backend, so the browser only
    // ever talks to one origin and CORS never enters the picture locally.
    // 8083 is Resibo's port in every environment — see resibo-plan.md §7.3.
    proxy: {
      '/api': {
        target: 'http://localhost:8083',
        changeOrigin: true,
      },
    },
  },
})
