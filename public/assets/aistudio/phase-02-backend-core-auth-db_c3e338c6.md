# Phase 02 – Backend Core, Database, Authentication

**Duration:** 5–6 days · **Depends on:** Phase 01 · **Read first:** 02-database-schema, 03-api-spec, 06-security

## Goal
Production-grade API foundation: schema + migrations on Neon, Firebase token verification, roles, user session endpoint, shop status endpoint, device tokens, account deletion.

## Tasks

### 2.1 Database
- [ ] Drizzle schema for **all** tables in 02-database-schema (types, enums, constraints, indexes, `updated_at` triggers)
- [ ] Migration workflow: `drizzle-kit generate` → review SQL → `migrate` using direct URL; run on dev, staging, prod via CI step
- [ ] Separate DB roles: `migrator` (DDL) and `app_runtime` (DML only); runtime uses the pooled URL
- [ ] Seed scripts (idempotent): shop_settings, categories (en + ta), sample products for dev/staging only, `seed:admin`
- [ ] Verify PITR/backups and test restoring into a new Neon branch

### 2.2 Auth plumbing
- [ ] Firebase Admin init from env. Auth plugin: parse Bearer token → `verifyIdToken` → attach `req.auth {firebaseUid, phone, email}`
- [ ] `requireUser` hook: upsert/load `users` row; block `is_deleted` users; attach `req.user {id, role}`
- [ ] `requireAdmin` hook: role = admin + `verifyIdToken(token, true)` (revocation check)
- [ ] App Check verification plugin (`X-Firebase-AppCheck`); enforce in staging/prod via `APP_CHECK_ENFORCE`
- [ ] `POST /auth/session`, `GET/PATCH /me`, `POST/DELETE /me/device-token`, `DELETE /me` (see account deletion in schema doc; deletes Firebase user via Admin SDK; require token `auth_time` within 5 minutes)

### 2.3 Shop status service
- [ ] `ShopService.getStatus()` computing `effectiveOpen = is_open || (reopen_at && reopen_at <= now)`; 15 s in-memory cache invalidated on update
- [ ] `GET /shop/status` public endpoint (fields per API spec incl. `serverTime`, `minSupportedVersion`)
- [ ] Unit tests for the effective-open logic (all combinations, boundary equality, timezone independence)

### 2.4 Cross-cutting
- [ ] Zod schemas in `packages/shared`; `.strict()` everywhere; common pagination helpers (cursor encode/decode)
- [ ] Standard error classes + localized messages (en/ta) via `Accept-Language`
- [ ] Rate limiting configuration per route group; request/response logging with redaction
- [ ] OpenAPI (non-production) via swagger plugin
- [ ] Legal URLs endpoint `/legal` (config driven)

### 2.5 Tests (integration, real Postgres on a Neon test branch or container)
- [ ] No token / bad token / expired token → 401
- [ ] New user upsert idempotent; role defaults to customer; client cannot set role via PATCH `/me`
- [ ] Customer token on `/admin/*` → 403; admin token OK; revoked admin token → 401
- [ ] Account deletion anonymises user, deletes addresses/device tokens, keeps orders redacted
- [ ] `/shop/status` correct for open / closed / reopen_at past / reopen_at future

## Verification commands
```
pnpm --filter backend db:generate && pnpm --filter backend db:migrate
pnpm --filter backend test
pnpm --filter backend seed && pnpm --filter backend seed:admin -- --phone +91XXXXXXXXXX
curl -H "Authorization: Bearer <test token>" https://api-dev.<domain>/v1/me
```

## Security checks
- Runtime DB user cannot run DDL; secrets only in env; no PII in logs (verify sample logs)
- Rate limit returns 429 with the standard error shape; unknown fields rejected with 422

## Acceptance criteria
- [ ] All tables migrated on dev/staging/prod; seeds run; admin seeded for dev/staging
- [ ] Auth, role and ownership tests pass; coverage on shop/auth modules >= 90%
- [ ] OpenAPI doc matches implemented endpoints
- [ ] Deployed to staging; `/v1/shop/status` works from a phone on mobile data
- [ ] Restore from backup demonstrated once

## Common pitfalls
- Trusting `phone_number` claim absence for Google users (phone may be null – schema allows it)
- Not handling Neon cold start: set sensible pool/connect timeouts and retry once on connection errors
- Verifying tokens against the wrong Firebase project per environment
