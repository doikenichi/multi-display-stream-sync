import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  server: {
    host: '0.0.0.0', // Binds to all network interfaces
    port: 3000,      // Sets the development server port
  },
  plugins: [react()],
})
