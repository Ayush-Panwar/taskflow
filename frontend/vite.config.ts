import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

// The dev server forwards API calls to the backend, so the browser only ever talks to one origin.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
});
