# 01 – Architecture

## 1. System diagram
```
 ┌─────────────────────────┐
 │ React Native app        │  Customer + Admin (role based)
 │ (Expo dev build)        │
 └───────┬─────────────────┘
         │ HTTPS, Authorization: Bearer <Firebase ID token>, X-App-Check-Token
         ▼
 ┌─────────────────────────┐        ┌──────────────────────┐
 │ Backend API (Fastify)   │───────▶│ Neon Postgres        │
 │  - auth middleware      │        │ (pooled connection)  │
 │  - zod validation       │        └──────────────────────┘
 │  - business logic       │───────▶ Firebase Admin (verify token, FCM, Storage)
 │  - rate limiting        │───────▶ Cashfree API + webhooks
 └─────────────────────────┘───────▶ Google Maps (Distance Matrix, optional)
```

Firebase handles identity, push and file storage. **Neon is the single source of truth for all business data.**

## 2. Authentication flow
1. App signs user in via Firebase (OTP or Google). Firebase SDK persists session securely.
2. For each API call the app attaches the current ID token (auto-refreshed by SDK) and an App Check token.
3. Backend verifies the ID token with Firebase Admin SDK (`verifyIdToken`; for admin routes use `checkRevoked: true`).
4. Backend upserts `users` by `firebase_uid`, loads `role` from DB (never trust client or token claims for role unless set via custom claims by backend).
5. Admin routes require `role = 'admin'`.

## 3. Request lifecycle (backend)
request → request-id → rate limit → security headers → auth → role guard → zod validation →
service layer (transaction where needed) → repository (Drizzle) → response mapper → error handler.

## 4. Layering (backend)
```
routes/        thin HTTP handlers (parse, call service, return)
services/      business rules (orders, pricing, geo, shop, notifications, payments)
repositories/  DB queries only
db/            schema, migrations, seeds, client
middleware/    auth, admin guard, rate limit, error handler, request id
lib/           firebase admin, cashfree (payments client), logger, config (env validated by zod), geo utils
```
Rules: routes never touch DB; services never touch HTTP objects; repositories contain no business rules.

## 5. Mobile layering
```
app/                 expo-router screens (thin)
src/features/<x>/    components, hooks, api calls, types per feature
src/components/      shared UI kit (design system)
src/services/        api client, firebase, notifications, location, storage
src/store/           zustand stores (cart, auth session, ui)
src/i18n/            en.json, ta.json
src/theme/           tokens
src/utils/           money, date, geo, validators
```
Server state (products, orders, shop status) via TanStack Query. Client state (cart, UI) via Zustand with persistence (AsyncStorage for cart only; never store tokens or PII there).

## 6. Shop status design
Single-row table `shop_settings` (id fixed to 1). Effective open state computed in SQL/service:
```
effective_open = is_open OR (reopen_at IS NOT NULL AND reopen_at <= now())
```
So admin can toggle OFF, set `reopen_at` = tomorrow 08:00, and the shop reopens automatically with no cron job.
Clients fetch `GET /shop/status` on app foreground, every 45 s on key screens (TanStack Query `refetchInterval`),
and on FCM data message `type=shop_status`. Order placement re-validates on the server.

## 7. Realtime strategy (no Firestore)
- Shop status: polling + FCM data push.
- Order status: FCM push + 15 s polling on tracking/order-details screen only while screen is focused.
- Admin new orders: FCM high-priority push to admin devices + 15 s polling on the orders screen.
- Future: SSE/WebSocket if live GPS is added.

## 8. Environments
| Env | Firebase project | Neon branch | Backend | App build |
|---|---|---|---|---|
| development | sunflowers-dev | `dev` | local / dev host | dev client |
| staging | sunflowers-staging | `staging` | staging host | internal APK/AAB |
| production | sunflowers-prod | `main` | prod host | Play Store AAB |

Each environment has its own secrets, Cashfree credentials (SANDBOX vs PRODUCTION), Google Maps keys (restricted per package + SHA-1).

## 9. Hosting
- Backend: a small always-on instance (Render/Railway/Fly.io/Cloud Run with min-instances 1). Free tiers that sleep are not acceptable for an ordering app.
- Neon: use the **pooled** connection string at runtime, direct string only for migrations. Plan: move to a paid tier before launch for longer point-in-time-restore history. Verify current limits.
- Health endpoint `/health` (checks DB) monitored by an uptime service; Sentry for errors.

## 10. Scalability notes
Expected load is small (single shop). Prioritise correctness and security over premature scaling.
Add indexes listed in schema, paginate lists, cache read-heavy catalog responses (ETag / short TTL).

## 11. Key decisions log
| # | Decision | Reason |
|---|---|---|
| D1 | Backend in front of Neon | Never expose DB credentials in APK |
| D2 | Money in paise integers | Avoid floating point errors |
| D3 | Server-side pricing | Prevent price tampering |
| D4 | Computed auto-reopen | No cron reliability issues |
| D5 | One app, role-based admin | Single Play Store listing, simpler review |
| D6 | Idempotency key on order create | Prevent duplicate orders on retry |
| D7 | Firebase App Check (Play Integrity) | Reduce abuse by non-genuine clients |
