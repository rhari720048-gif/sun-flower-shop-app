# Phase 09 – Security Hardening, Performance, Accessibility, i18n Polish

**Duration:** 5 days · **Depends on:** Phase 08

## Goal
Take the working app to production quality. Close every item in `06-security.md`, hit performance budgets, pass accessibility and language review.

## Security tasks
- [ ] Walk **06-security.md** line by line; record evidence (screenshot/command/test name) for each item in `docs/security-evidence.md`
- [ ] Enforce App Check (Play Integrity) for backend and Firebase services in staging then production; verify requests without a token are rejected
- [ ] Release build hardening: Hermes, R8/ProGuard (minify + shrink resources), disable debug, `allowBackup=false`, `usesCleartextTraffic=false`; review merged manifest and **remove unnecessary permissions**
- [ ] Run **MobSF** (or equivalent) on the release APK/AAB and triage findings; grep APK for secrets/URLs (`apkanalyzer`, `strings`)
- [ ] Dependency review: audit, remove unused packages, licence check
- [ ] API pen-test script (Postman/Newman or k6): IDOR, auth bypass, mass assignment, rate limits, price tamper, radius tamper, replay, forged payment signature
- [ ] Backend: security headers verified (securityheaders-style check), TLS configuration test, CORS check, body size limits, timeouts
- [ ] Verify logs contain no PII/tokens; Sentry/Crashlytics scrubbers configured
- [ ] Database: runtime role privileges, SSL enforced, backup/restore rehearsal, retention job for stale device tokens
- [ ] Write `docs/runbook.md` (incident steps: stop orders via toggle, rotate secrets, revoke sessions, restore DB, contacts)

## Performance tasks
- [ ] Cold start profiling (Hermes, lazy-load heavy screens/maps, avoid large synchronous work at startup, preload fonts)
- [ ] List performance: FlashList/FlatList tuning, memoised rows, image sizes; measure with Flipper/Perfetto on a 2 GB device
- [ ] Bundle/AAB size review (remove unused assets/fonts weights, enable `enableProguardInReleaseBuilds`, split ABIs via AAB)
- [ ] Backend: query plans (`EXPLAIN ANALYZE`) for hot queries; indexes confirmed; p95 measured with k6 at expected peak (e.g. festival load: 50 concurrent users)
- [ ] Network: gzip/br compression, caching headers, request de-duplication, minimal payloads

## Accessibility tasks
- [ ] TalkBack pass for login, browse, cart, checkout, order tracking: labels, roles, focus order, announcements for status changes
- [ ] Contrast audit (4.5:1), font scaling 1.3x without clipping, touch targets 48 dp, no colour-only meaning (use icons/text for status)
- [ ] Reduce-motion respected

## i18n polish
- [ ] Native Tamil speaker review of every string and notification; fix overflow; pluralisation; date/number formats (`en-IN`, IST)
- [ ] Verify key parity test and no hard-coded strings

## Verification
```
pnpm -r lint typecheck test
newman run security-suite.postman.json -e staging.env.json
k6 run load/orders.js
./gradlew bundleRelease && apkanalyzer ...
```

## Acceptance criteria
- [ ] Zero unresolved High/Critical findings from MobSF, audit, and manual pen-test list; Mediums triaged with documented decisions
- [ ] Cold start < 3 s and 60 fps list scrolling on a mid/low-end device; AAB < 40 MB
- [ ] p95 API < 400 ms under load test; no errors at 50 concurrent users
- [ ] TalkBack can complete an order; Tamil review signed off
- [ ] `security-evidence.md` complete and `runbook.md` exists

## Pitfalls
- Enabling R8 and breaking reflection-based libs (test the release build thoroughly); enforcing App Check before all client builds support it; fixing performance only on a flagship phone
