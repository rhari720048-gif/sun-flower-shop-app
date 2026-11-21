# Phase 01 – Foundation, Environments, Tooling

**Duration:** 3–4 days · **Depends on:** nothing · **Read first:** README, 01-architecture, 05-engineering-standards

## Goal
A clean, secure, reproducible base: accounts, repos, three environments, CI, and an empty app + empty API that deploy end to end.

## Inputs needed from the owner
- Final app name, brand assets (logo SVG/PNG), domain name (for API, policy pages, email)
- Google account for Firebase/Play, business details for Cashfree later
- Shop GPS coordinates (can be provided in Phase 5)

## Tasks

### 1.1 Accounts and access (all with 2FA)
- [ ] GitHub organisation/repo (private), branch protection on `main`, Dependabot + secret scanning on
- [ ] Firebase: create **three projects** (dev, staging, prod). Enable Authentication (Phone, Google), Cloud Messaging, Storage, Crashlytics, App Check
- [ ] Neon: one project, branches `main` (prod), `staging`, `dev`; note pooled vs direct connection strings
- [ ] Google Play Console developer account (start identity verification now; it can take days)
- [ ] Cashfree merchant account created, sandbox keys generated, KYC/activation started (can finish later); Google Cloud project with Maps SDK, Places, Geocoding enabled (billing on, budget alert set)
- [ ] Hosting account for backend (Render/Railway/Fly/Cloud Run), domain DNS access, Sentry project, uptime monitor
- [ ] Password manager vault with shared recovery notes (owner + developer)

### 1.2 Repository setup (pnpm monorepo)
```
/mobile  /backend  /packages/shared  /docs  /.github/workflows
```
- [ ] TypeScript strict base config; ESLint + Prettier; Husky + lint-staged + commitlint; `.editorconfig`
- [ ] `.gitignore` (env files, keystores, google-services.json for non-dev, build outputs)
- [ ] `docs/` copied from this folder; `CHANGELOG.md`; `CONTRIBUTING.md`; ADR folder

### 1.3 Backend skeleton
- [ ] Fastify + TypeScript + `fastify-type-provider-zod`, pino logger with redaction, helmet, rate-limit, cors (restricted), sensible, graceful shutdown
- [ ] `config.ts` validating env with Zod (fail fast): `NODE_ENV, PORT, DATABASE_URL, DATABASE_URL_DIRECT, FIREBASE_PROJECT_ID, FIREBASE_CLIENT_EMAIL, FIREBASE_PRIVATE_KEY, CASHFREE_CLIENT_ID, CASHFREE_CLIENT_SECRET, CASHFREE_ENV (SANDBOX|PRODUCTION), CASHFREE_API_VERSION, SENTRY_DSN, APP_CHECK_ENFORCE`
- [ ] `GET /health` (db ping), request-id plugin, global error handler returning the standard error shape
- [ ] Dockerfile (multi-stage, non-root user), `.env.example`
- [ ] Deploy to dev and staging hosts; custom domain + HTTPS verified

### 1.4 Mobile skeleton
- [ ] `create-expo-app` with TypeScript, expo-router, dev client; set `applicationId` (e.g. `in.sunflowers.app`), scheme, app name
- [ ] Add `@react-native-firebase/app` (+ auth, messaging, crashlytics, app-check) with config plugins; `google-services.json` per environment via EAS secrets/file env
- [ ] EAS: `eas.json` profiles `development`, `staging`, `production`; env vars per profile; build a dev client APK and run on a real device
- [ ] Add SHA-1/SHA-256 of debug and EAS keystores to each Firebase Android app (needed for OTP/Google sign-in). Add Play App Signing SHA-256 later (Phase 11)
- [ ] Folder structure from 01-architecture; path aliases; i18n scaffolding (en/ta); theme tokens file; fonts loaded (Playfair Display, Poppins, Noto Sans Tamil)
- [ ] Root error boundary, Crashlytics test crash verified

### 1.5 Shared package
- [ ] `packages/shared`: Zod schemas and types for error format, shop status, money utils (`formatPaise`), enums (order status, roles). Consumed by backend and mobile.

### 1.6 CI/CD
- [ ] Workflow: install (cached), lint, typecheck, unit tests, gitleaks, audit on PR
- [ ] Workflow: backend deploy on merge to `develop` (dev) and tag (prod); DB migration step placeholder
- [ ] Workflow: EAS build trigger on tag (manual approval for production)

## Verification commands
```
pnpm -r lint && pnpm -r typecheck && pnpm -r test
curl https://api-dev.<domain>/health   # {"status":"ok","db":"ok"}
eas build --profile development --platform android
```

## Security checks
- No secret committed (gitleaks clean). Firebase service account only in host secrets. Maps key restricted.
- Branch protection active; 2FA verified on all accounts.

## Acceptance criteria (Definition of Done)
- [ ] Fresh clone → run in <= 5 documented commands
- [ ] Dev and staging API deployed over HTTPS, health green, logs visible, Sentry receiving a test error
- [ ] Dev client installs on a physical Android phone and shows a styled placeholder home with correct fonts; Crashlytics test crash appears in console
- [ ] CI green on a sample PR; blocked on failing lint/test
- [ ] All three environments separated (different Firebase projects, DB branches, secrets)

## Common pitfalls
- Using Expo Go with React Native Firebase (not supported → dev build required)
- Forgetting SHA-256 for Google/OTP sign-in → silent auth failures
- Using the direct Neon URL at runtime (connection exhaustion) – use the pooler
- Committing `google-services.json` for prod without thinking; treat as config, but keep per-env files via EAS
