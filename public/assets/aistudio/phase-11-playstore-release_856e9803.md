# Phase 11 – Play Store Compliance and Release

**Duration:** 4 days of work + closed-test waiting period (14 days if required) · **Depends on:** Phase 10 · **Read first:** 08-playstore-checklist, 09-legal-templates

## Goal
Publish to Google Play and get approved on the first review, then roll out safely.

## Tasks
### 11.1 Legal and web presence
- [ ] Publish Privacy Policy, Terms, Refund/Cancellation, Shipping, **Account Deletion** pages (English + Tamil) on the domain; legal review done; links wired in the app (`/legal`)
- [ ] Simple business website (home, contact, policies) – also helps Cashfree live approval
- [ ] Owner fills real contact info, grievance officer details

### 11.2 Store assets
- [ ] App icon 512x512, feature graphic 1024x500, 6–8 phone screenshots (English and Tamil sets) taken from the release build with real data
- [ ] Short description, full description, release notes (EN/TA); localisation for Tamil listing
- [ ] Content rating, target audience, category and tags prepared in a `docs/store-listing.md`

### 11.3 Build and signing
- [ ] Production env variables/secrets set; Cashfree **production** credentials + webhook URL configured (or keep COD-only for first release)
- [ ] Final production build via EAS (AAB), Play App Signing enrolled; upload key backed up in two places
- [ ] Add the **Play App Signing SHA-1/SHA-256** to Firebase (Auth/Google sign-in/Maps API key restrictions) – otherwise login breaks in the Play build
- [ ] Verify target API level and permissions in the final merged manifest; Crashlytics/Sentry mapping uploaded

### 11.4 Play Console setup
- [ ] Create app; complete **App content** forms (privacy policy, ads = none, app access with reviewer credentials, content rating, target audience, data safety, data deletion URL, financial features, health etc.)
- [ ] Data Safety answers cross-checked against the actual SDK list and the privacy policy (`docs/data-safety-matrix.md`)
- [ ] Reviewer instructions: test phone number + OTP (Firebase test number), a test address inside the radius, how to place a COD test order, note that the shop is OPEN during review; optional admin test credentials
- [ ] Keep the shop **open** and a few products in stock during review; consider a "reviewer" test account flagged server-side to bypass the delivery-hours rule only (never radius/auth)

### 11.5 Testing tracks
- [ ] Upload to **Internal testing** first; install via Play link; smoke test (login, order, push, deletion)
- [ ] **Closed testing**: recruit >= 12 testers (more is safer) and keep them opted in for 14 continuous days if the account requires it (verify); collect feedback; fix issues and upload new builds as needed
- [ ] Review **Pre-launch report** and Android vitals; fix crashes/ANRs/accessibility warnings

### 11.5b Production
- [ ] Apply for production access (if required), submit production release with **staged rollout** (10% → 50% → 100% across several days)
- [ ] Monitor Crashlytics, Play Vitals, Sentry, backend logs daily during rollout; halt rollout on regression

## Verification checklist (before pressing "Submit")
Run through every checkbox in `08-playstore-checklist.md`; attach evidence (screenshots of forms) in `docs/playstore-evidence/`.

## Acceptance criteria
- [ ] App approved and live on Google Play; install from Play works on 3 devices; login works with Play-signed build
- [ ] No policy warnings in Play Console; Data safety matches behaviour
- [ ] Staged rollout reached 100% with crash-free sessions >= 99.5%
- [ ] Owner trained: shop toggle, order flow, adding products, handling support (short Tamil video/guide)

## Common rejection causes and prevention
| Cause | Prevention |
|---|---|
| Reviewer cannot log in | Firebase test number + clear instructions |
| Shop closed during review | Keep it open; mention in notes |
| Missing/invalid privacy policy or deletion URL | Public, working HTTPS pages |
| Data safety mismatch | Matrix of SDKs vs declarations |
| Crash on launch (release-only) | Test Play internal build, R8 rules, Play SHA in Firebase |
| Misleading screenshots/metadata | Real UI only, accurate claims |
| Wrong target API / permissions | Merged manifest audit |
