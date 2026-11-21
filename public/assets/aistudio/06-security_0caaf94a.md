# 06 – Security Requirements

Reference: OWASP MASVS (L1 + selected L2), OWASP Mobile Top 10, OWASP API Security Top 10, India DPDP Act 2023.
Every item is a release blocker unless marked optional.

## 1. Secrets and configuration
- [ ] No secrets in the app bundle. Only public identifiers: API base URL, Firebase client config, restricted Google Maps key, Cashfree environment flag only (never the client secret; the app receives a short-lived `payment_session_id` per order).
- [ ] Backend secrets (DATABASE_URL, Firebase service account, Cashfree client id + client secret (also used for webhook signature verification)) only in host env/secret manager. Never in git. `.env` ignored; `.env.example` committed.
- [ ] Separate secrets per environment; rotate on suspicion and every 6 months.
- [ ] gitleaks in CI and pre-commit. If a secret ever leaks: rotate immediately, purge from history.
- [ ] Google Maps key restricted to Android package name + SHA-1 (debug, release, Play App Signing SHA). Server-side keys restricted by IP/API.

## 2. Authentication and session
- [ ] Firebase Auth phone OTP + Google. Enable Firebase App Check (Play Integrity) and enforce for Auth/Storage once stable.
- [ ] Backend verifies ID token on every request (signature, audience = project id, issuer, expiry). Admin routes use `checkRevoked: true`.
- [ ] Roles stored in DB only; client cannot set role. First admin seeded via script. Optional: mirror role as Firebase custom claim set by backend only.
- [ ] Sensitive actions (delete account) require recent sign-in (re-authentication within 5 min).
- [ ] Logout: delete FCM token on server, sign out of Firebase, clear query cache and cart store.
- [ ] OTP abuse: App Check + Firebase built-in quotas; add backend rate limiting on `/auth/session`; configure SMS region policy to India only if possible.

## 3. Authorization (API Top 10: BOLA/BFLA)
- [ ] Every resource query includes ownership (`WHERE user_id = :currentUser`). Automated tests prove user A cannot read/modify user B's orders, addresses, wishlist.
- [ ] Admin endpoints guarded by middleware, tested with a customer token (must return 403).
- [ ] Mass assignment prevented: Zod schemas are strict (`.strict()`), explicit field whitelists on PATCH.
- [ ] Order status transitions enforced server-side by a state machine; customer cannot set arbitrary status.

## 4. Input and output
- [ ] Zod validation on body, query, params, headers. Reject unknown fields. Max body size 100 KB (uploads go straight to Storage via signed URL).
- [ ] Parameterised queries only. No string-built SQL.
- [ ] Output mapping: never leak internal fields (firebase_uid, other users' phones, audit data).
- [ ] Sanitise text shown in notifications/admin screens (React Native renders text safely; no `dangerouslySetInnerHTML`/WebView of user content).
- [ ] File uploads: allow jpeg/png/webp only, <= 3 MB, verify magic bytes server-side if proxied, random filenames, Storage rules restrict write to admin and read to public for product images only.

## 5. Transport and network
- [ ] HTTPS only; HSTS on API; TLS 1.2+. Android `usesCleartextTraffic=false`. Network security config blocks user-installed CAs in release (optional hardening).
- [ ] Certificate pinning: **optional**; if used, pin backup key and have rotation plan (a bad pin can brick the app).
- [ ] CORS locked down (mobile does not need it; only allow admin web origin if any).
- [ ] Security headers via `@fastify/helmet`. Rate limiting via `@fastify/rate-limit` (see API spec). Request timeouts, body limits.

## 6. Payments (Cashfree)
- [ ] Cashfree order created **server-side only** (`POST /pg/orders` with `x-client-id`, `x-client-secret`, `x-api-version`); amount computed on the server. Client never sends amount. Cashfree takes `order_amount` in **rupees with 2 decimals**: convert from paise with integer math (`(paise/100).toFixed(2)` from integers, never float arithmetic) and unit-test it.
- [ ] Client receives only `payment_session_id` + `order_id`; session ids are never logged.
- [ ] Never mark paid from the app callback. After the SDK callback (and on app relaunch), backend calls Cashfree **Get Order / Get Payments** and marks paid only if the status from Cashfree is PAID and the amount equals our `total_paise`.
- [ ] Webhook: verify `x-webhook-signature` = Base64(HMAC-SHA256(`x-webhook-timestamp` + **raw body**, client secret)) with constant-time compare; reject if headers missing/mismatch or timestamp too old; store `event_id` for idempotency; handle payment success, failed, user-dropped and refund status events. Capture raw body before JSON parsing.
- [ ] Optionally restrict webhook endpoint by Cashfree's published IPs (verify current list) in addition to the signature.
- [ ] Order expiry set on the Cashfree order (`order_expiry_time`) matching `payment_expires_at`; expired/unpaid orders auto-cancel.
- [ ] No card data touches our servers or logs. Use the SDK's web checkout / UPI intent flows (or Cashfree's card component) so raw card numbers are never handled by our code, keeping PCI scope minimal.

## 4b. Data protection and privacy (DPDP Act 2023 aligned)
- [ ] Collect only what is needed: name, phone, delivery address/location, order data, device token.
- [ ] Explicit consent screen text linking Privacy Policy and Terms at sign-up; log consent version/time (optional table `user_consents`).
- [ ] Location used only while app is in use, only for address selection; never collected in the background; coordinates stored only for saved addresses/orders.
- [ ] Encryption in transit (TLS) and at rest (Neon default encryption at rest). Backups encrypted.
- [ ] Data subject rights: view/edit profile in-app, delete account in-app (+ web deletion request page), contact for grievance in policy.
- [ ] Logs: no PII/tokens/addresses; retention <= 30 days.
- [ ] No third-party trackers beyond Firebase Crashlytics/Analytics; disclose all in Data Safety form. Provide analytics opt-out if analytics enabled.

## 7. App hardening (mobile)
- [ ] Release build: Hermes bytecode, R8/ProGuard minify + shrink enabled, source maps uploaded privately (Crashlytics/Sentry), debug off, `android:allowBackup="false"`, `android:debuggable` false.
- [ ] No sensitive data in AsyncStorage (only cart items and language). Tokens handled by Firebase SDK (Keystore-backed). Use `expo-secure-store` for anything else sensitive.
- [ ] No sensitive data in logs, screenshots of OTP not blocked (not needed), clipboard not used for secrets.
- [ ] Deep links: `https` app links with `assetlinks.json`, validate all params; no auto-actions from intents.
- [ ] Exported components minimised; only launcher activity exported. Intent filters reviewed.
- [ ] Permissions minimal: INTERNET, ACCESS_NETWORK_STATE, ACCESS_FINE/COARSE_LOCATION (foreground), POST_NOTIFICATIONS, VIBRATE (haptics), CALL_PHONE **not requested** (use dialer intent `tel:` which needs no permission). Remove all unused permissions injected by libraries (verify merged manifest).
- [ ] Root/jailbreak detection and screenshot blocking: not required for this app (document decision).
- [ ] Update prompt: Play In-App Updates (flexible) + server-driven minimum version (`GET /shop/status` returns `minSupportedVersion`) to force update on breaking API changes.

## 8. Infrastructure
- [ ] Neon: separate roles (migrator, app runtime), IP allow-list if available, SSL required, pooled connection for runtime, branches per env, PITR enabled, restore tested.
- [ ] Backend host: non-root container, minimal image, auto restarts, secrets via platform vault, only needed ports.
- [ ] GitHub: branch protection, 2FA for all accounts, least-privilege tokens, Dependabot + secret scanning enabled.
- [ ] Google Play Console, Firebase, Neon, Cashfree, domain registrar: 2FA mandatory, shared owner recovery documented.
- [ ] Upload keystore: with Play App Signing, keep upload key backed up in two secure places.

## 9. Monitoring and incident response
- [ ] Crashlytics + Sentry alerts, uptime alerts, error-rate alert, failed-payment alert.
- [ ] Audit log of every admin write (who, what, when).
- [ ] Incident runbook (`docs/runbook.md`): how to disable orders (shop toggle), rotate secrets, revoke Firebase sessions, restore DB, contact list. Data breach: notify per DPDP obligations.

## 10. Security test plan (run in Phase 9/10)
- Static: ESLint security, `npm audit`, gitleaks, MobSF scan of release APK.
- Dynamic: Postman/Burp tests for IDOR, missing auth, mass assignment, rate limits, replay of order creation, tampered price, tampered radius coordinates, expired/forged tokens.
- Business-logic abuse: order when closed, order out of radius via modified lat/lng (server recalculates from stored address; address creation also validated), negative/huge quantities, duplicate idempotency keys, illegal status transitions.
- Payment tests: forged signature, replayed webhook, amount mismatch.
