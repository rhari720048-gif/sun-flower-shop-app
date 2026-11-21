# Phase 03 – Mobile App Shell, Design System, Auth UI

**Duration:** 5–6 days · **Depends on:** Phase 02 · **Read first:** 04-design-system, 05-engineering-standards

## Goal
A polished app skeleton that matches the mockup: splash, onboarding, login (OTP + Google), profile completion, bottom tabs, UI kit, i18n, API client, session handling.

## Tasks

### 3.1 UI kit (build before screens)
- [ ] Theme tokens (colours, spacing, radius, shadows, typography) exactly from 04-design-system
- [ ] Components: Button, IconButton, TextInput, OtpInput, Chip, Badge, SectionHeader, EmptyState, ErrorState, SkeletonLoader, Toast, BottomSheet, ConfirmDialog, StatusBanner, Avatar
- [ ] Storybook-like internal "UI gallery" screen (dev only, excluded from production builds) to visually verify every component state
- [ ] Icon set (one library, e.g. lucide/feather via react-native-svg), consistent stroke

### 3.2 Infrastructure
- [ ] API client (see standards §5): token + App Check attach, retry/backoff for GET, 401 refresh-then-signout, timeouts, typed errors
- [ ] TanStack Query provider with sensible defaults (staleTime, retry, focus refetch, offline persistence for catalogue later)
- [ ] Network status hook + global offline banner
- [ ] i18n (i18next + expo-localization): en/ta, language switch persisted, Tamil font fallback
- [ ] Remote/forced update check using `minSupportedVersion` from `/shop/status`
- [ ] Auth store/session hook: Firebase `onAuthStateChanged`, `POST /auth/session` after login, role in memory

### 3.3 Screens
- [ ] **Splash**: assets from mockup; runs bootstrap (fonts, auth state, shop status) then routes; max wait handling with timeout + retry state
- [ ] **Onboarding** (single or 3 slides, skippable, shown once)
- [ ] **Login**: phone input with +91, validation, "Send OTP"; **OTP screen** with auto-fill (Android SMS Retriever via Firebase), resend timer 30 s, attempts/errors localized (invalid code, too many requests, network); **Google Sign-In**
- [ ] Consent text with links to Terms/Privacy (open in-app browser)
- [ ] **Profile completion** (name required) → `PATCH /me`
- [ ] **Tabs shell**: Home, Track, Wishlist, Account with icons and active state per mockup; drawer (hamburger) with About/Contact/Policies/Language
- [ ] **Account** basic: language switch, logout, policy links, app version (delete account UI comes in Phase 8)
- [ ] Logout flow: remove device token, `signOut`, clear query cache and stores

### 3.4 Quality
- [ ] Accessibility labels/roles on all controls; touch targets >= 48 dp; font scale 1.3 verified
- [ ] Unit/component tests: Button, OtpInput, formatters, i18n key parity test (en.json and ta.json have identical keys)
- [ ] Maestro flow: fresh install → login with Firebase test number → profile name → lands on Home

## Verification
```
pnpm --filter mobile lint typecheck test
maestro test .maestro/auth.yaml
eas build --profile staging --platform android   # install and test on 2 physical devices
```

## Security checks
- No token/OTP in logs; Crashlytics scrubbing; no sensitive data in AsyncStorage; screens do not leak data in app switcher beyond product info (no special handling needed)

## Acceptance criteria
- [ ] UI matches the mockup (visual review against the three supplied screens)
- [ ] OTP and Google login work on release-like staging build, including after app restart (session persisted) and logout
- [ ] Offline banner and API error states demonstrated
- [ ] Tamil/English switch works everywhere implemented so far
- [ ] No hard-coded strings/colours (lint rule or grep check passes)

## Common pitfalls
- Phone auth failing on release builds due to missing SHA-256 / App Check configuration
- Letting the splash block forever when offline – always provide fallback and retry
- Tamil text clipped due to fixed line heights
