# 05 – Engineering Standards (senior-developer quality bar)

The goal: the codebase must look hand-crafted by an experienced engineer. Reviewers should find consistent
architecture, meaningful names, real error handling and no generated-looking filler.

## 1. Repository
Monorepo (pnpm workspaces) or two repos. Recommended monorepo:
```
/mobile  /backend  /docs  /packages/shared (zod schemas + types shared by both)
```
Branching: `main` (protected), `develop`, feature branches `feat/<scope>-<short>`. PRs required, 1 review (self-review checklist if solo), CI green before merge.
Commits: Conventional Commits (`feat(orders): ...`). Semantic version tags per release (`v1.0.0`).

## 2. TypeScript and lint
- `strict: true`, `noUncheckedIndexedAccess`, `noImplicitOverride`. No `any` (lint error) except isolated, commented cases.
- ESLint (typescript-eslint strict, react-hooks, react-native, import order, security plugin) + Prettier. Husky + lint-staged pre-commit; commitlint.
- Path aliases (`@/components`), no deep relative imports (`../../../`).

## 3. Code style rules
- Small functions (<= 40 lines), single responsibility, early returns, no nested ternaries.
- Names describe intent (`calculateOrderTotals`, not `calc`). No abbreviations except common ones (id, url).
- Comments explain WHY, not WHAT. No commented-out code. No tutorial-style comments ("// this function does...").
- No magic numbers/strings: constants file or settings.
- No dead code, unused exports, unused deps (run `knip`/`depcheck`).
- Consistent error handling: custom `AppError(code, status, message)`; services throw, global handler maps to response.
- Async: always handle rejections; no floating promises (lint rule).
- Pure functions for money/geo/pricing with unit tests.
- Date/time via `date-fns` + `date-fns-tz`; never manual timezone math.

## 4. React Native specific
- Functional components + hooks, memoize list items (`React.memo`), `FlatList`/`FlashList` with `keyExtractor`, `getItemLayout` where possible.
- Images via `expo-image` with caching, placeholders (blurhash), correct sizes (never load 4000 px images; admin uploads are compressed and served resized).
- No inline style objects in hot lists; use `StyleSheet.create` or tokens.
- Avoid re-renders: selector-based Zustand, stable callbacks.
- Handle keyboard (KeyboardAvoidingView), safe areas, Android back button, app state changes (foreground refetch).
- Hermes enabled; New Architecture per Expo SDK default; verify each native lib supports it.
- Error boundary at root + per-route; report to Crashlytics.
- Permissions requested just-in-time with rationale.

## 5. API client (mobile)
Single axios/fetch wrapper: base URL from env, attaches ID token + App Check token, retries idempotent GETs with backoff, maps API errors to typed `ApiError`, handles 401 (force token refresh once, then sign out), timeouts (15 s), cancels on unmount. All endpoints typed via shared Zod schemas; responses parsed/validated at the boundary.

## 6. Backend rules
- Env validated at boot with Zod (`config.ts`); app refuses to start with missing/invalid config.
- Structured JSON logs (pino) with request id; redact `authorization`, phone, address, tokens.
- Repository pattern, transactions for multi-write operations, parameterised queries only (Drizzle).
- Graceful shutdown (SIGTERM), health/readiness endpoints, connection pooling, query timeouts.
- Idempotent webhook processing, safe retries.
- Migrations are forward-only, reviewed, run in CI/CD before deploy; each migration tested on a Neon branch copy.

## 7. Documentation inside repo
`README` per package (setup in 5 commands), `.env.example` (no secrets), `docs/adr/` short Architecture Decision Records, `CHANGELOG.md`, `CONTRIBUTING.md`.

## 8. Dependency policy
Pin versions (lockfile committed). Add a dependency only with justification; prefer maintained libs (check last release, issues, downloads). `npm audit`/`pnpm audit` and Dependabot weekly. Check licences (no GPL in app).

## 9. CI pipeline (GitHub Actions)
On every PR: install → lint → typecheck → unit tests → backend integration tests (Neon branch or Postgres service container) → build check (`expo export` / `tsc`) → secret scan (gitleaks) → audit.
On tag: EAS build (production profile) → artifact; backend deploy after migrations.

## 10. Anti "AI-generated look" rules
- No lorem ipsum, no "Item 1/Item 2", no placeholder images in committed UI. Use real seeded data.
- No generic "Something went wrong" everywhere; messages are specific and localized with next action.
- No over-commenting, no boilerplate files that are unused, no emoji in code/log messages.
- Consistent patterns across features (same folder layout, same hook naming `useX`, same error flow).
- Real product copy written for flower-shop customers (Tamil + English), reviewed.
- Pixel-level polish: alignment to 4 px grid, consistent icon set (one library), no mixed icon styles.
