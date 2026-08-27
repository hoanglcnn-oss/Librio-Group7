import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/auth': 'http://localhost:8080',
      '/resources': 'http://localhost:8080',
      '/me': 'http://localhost:8080',
      '/borrow-requests': 'http://localhost:8080',
      '/librarian': 'http://localhost:8080',
    },
  },
})
