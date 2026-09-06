import { defineConfig } from '@playwright/test'

const frontendPort = process.env.PLAYWRIGHT_FRONTEND_PORT || '5173'
const backendPort = process.env.PLAYWRIGHT_BACKEND_PORT || '18080'
const frontendHost = process.env.PLAYWRIGHT_FRONTEND_HOST || '127.0.0.1'
const backendHost = process.env.PLAYWRIGHT_BACKEND_HOST || '127.0.0.1'
const baseURL = process.env.PLAYWRIGHT_BASE_URL || `http://${frontendHost}:${frontendPort}`
const backendURL = process.env.PLAYWRIGHT_BACKEND_URL || `http://${backendHost}:${backendPort}`

export default defineConfig({
  testDir: './e2e',
  outputDir: 'test-results',
  timeout: 45_000,
  expect: {
    timeout: 8_000,
  },
  workers: 1,
  fullyParallel: false,
  use: {
    baseURL,
    video: {
      mode: 'on',
      size: { width: 1280, height: 720 },
    },
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
    viewport: { width: 1280, height: 720 },
    acceptDownloads: true,
  },
  reporter: [
    ['list'],
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
  ],
  webServer: [
    {
      command: 'powershell -NoProfile -ExecutionPolicy Bypass -File scripts/start-e2e-backend.ps1',
      url: `${backendURL}/health`,
      reuseExistingServer: false,
      timeout: 120_000,
    },
    {
      command: `npm run dev -- --host ${frontendHost} --port ${frontendPort}`,
      url: baseURL,
      reuseExistingServer: false,
      timeout: 60_000,
      env: {
        VITE_API_BASE_URL: backendURL,
        VITE_USE_MOCK_BORROWINGS: 'false',
        VITE_USE_MOCK_LIBRARIAN_BORROWINGS: 'false',
        VITE_USE_MOCK_DIGITAL_ACCESS: 'false',
        VITE_USE_MOCK_RESOURCE_ADMIN: 'false',
      },
    },
  ],
})
