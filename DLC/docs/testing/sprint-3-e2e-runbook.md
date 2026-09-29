# Sprint 3 E2E Runbook

## Prerequisites

- Java 17.
- Maven 3.9.x, either on `PATH` as `mvn.cmd` or passed through `MAVEN_CMD`.
- Node.js and npm.
- Chromium browser installed by Playwright: `npx playwright install chromium`.

## Environment Variables

- `PLAYWRIGHT_BASE_URL`: frontend URL, default `http://127.0.0.1:5173`.
- `PLAYWRIGHT_BACKEND_URL`: backend URL, default `http://127.0.0.1:18080`.
- `PLAYWRIGHT_FRONTEND_PORT`: default `5173`.
- `PLAYWRIGHT_BACKEND_PORT`: default `18080`.
- `MAVEN_CMD`: optional full path to `mvn.cmd`.
- `JAVA_HOME`: optional Java 17 path.
- `LIBRIO_E2E_DRIVE`: optional mapped drive letter for Windows Unicode path workaround, default `X`.

Frontend mock flags are forced to `false` by Playwright webServer config.

## Dedicated E2E Database

The backend starts with Spring profile `e2e`. That profile uses a dedicated H2 PostgreSQL-mode database named `librio-e2e` and seeds deterministic E2E fixtures at startup.

The E2E seeder has a guard: it resets data only when the JDBC URL contains `librio-e2e`. Do not point the `e2e` profile at a shared production or development database.

## Start Backend

Normally Playwright starts the backend automatically:

```powershell
cd frontend
npm run e2e
```

Manual start, useful for debugging:

```powershell
subst X: "<repo-root>"
cd X:\backend
$env:SPRING_PROFILES_ACTIVE="e2e"
$env:SERVER_PORT="18080"
& "C:\Users\Admin\apache-maven-3.9.8\bin\mvn.cmd" spring-boot:run "-Dspring-boot.run.profiles=e2e"
```

## Start Frontend

Playwright also starts Vite automatically. Manual start:

```powershell
cd frontend
$env:VITE_API_BASE_URL="http://127.0.0.1:18080"
$env:VITE_USE_MOCK_BORROWINGS="false"
$env:VITE_USE_MOCK_LIBRARIAN_BORROWINGS="false"
$env:VITE_USE_MOCK_DIGITAL_ACCESS="false"
$env:VITE_USE_MOCK_RESOURCE_ADMIN="false"
npm run dev -- --host 127.0.0.1 --port 5173
```

## Run E2E Review

```powershell
cd frontend
npm ci
npx playwright install chromium
npm run e2e
```

Headed review mode:

```powershell
npm run e2e:review
```

Run one spec:

```powershell
npx playwright test e2e/return.spec.js
```

## Evidence

- Videos: `frontend/test-results/**/video.webm`
- HTML report: `frontend/playwright-report/index.html`
- Trace on failure: `frontend/test-results/**/trace.zip`
- Screenshots on failure: `frontend/test-results/**/*.png`

Open report:

```powershell
npm run e2e:report
```

Open a trace:

```powershell
npx playwright show-trace test-results\<spec-folder>\trace.zip
```

Generated evidence is ignored by Git. Upload or archive it outside the repo for Sprint Review if needed.

## Clean-Clone Localhost Fallback

1. Clone the repo and checkout `feature/sprint3-backend-db`.
2. Run `cd frontend; npm ci; npx playwright install chromium`.
3. Set `MAVEN_CMD` if Maven is not on `PATH`.
4. Run `npm run e2e`.
5. If the workspace path contains Unicode and Maven fails, let `scripts/start-e2e-backend.ps1` map the repo to `X:` or set `LIBRIO_E2E_DRIVE` to a free drive letter.
