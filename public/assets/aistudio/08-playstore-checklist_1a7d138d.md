# 08 – Google Play Store Checklist

> Policies change often. Before submission, re-check the current Play Console Help/Policy Center and the
> "App content" page in the console. Items marked (verify) have changed in recent years.

## 1. Developer account
- [ ] Google Play Developer account created (one-time registration fee), 2-step verification on, identity verified.
- [ ] Account type decision: **Personal** vs **Organization**. For personal accounts created after Nov 2023, production access requires a **closed test with at least 12 testers opted-in for 14 continuous days** (verify current numbers). Plan this in Phase 11.
- [ ] Complete developer contact details (public email, phone for some regions), website (optional but recommended).
- [ ] Check Google's upcoming **developer identity verification for sideloading/all apps** rollout timeline and complete when asked (verify).

## 2. App identity and build
- [ ] Unique `applicationId` (e.g. `in.sunflowers.app`), never changes after publish.
- [ ] Upload **AAB** (not APK). Enrol in **Play App Signing**. Keep the upload keystore backed up safely.
- [ ] `versionCode` increments every upload; `versionName` semantic.
- [ ] **Target API level** meets the current Play requirement (new apps must target the latest or latest-1 Android API; verify deadline) and `minSdk` 24 (Android 7) or higher justified.
- [ ] 64-bit support included (RN/Expo does this by default).
- [ ] Release build tested from Play internal track (not just local APK).
- [ ] Merged `AndroidManifest.xml` reviewed: no unused/dangerous permissions, no exported components accidentally, `allowBackup=false`.
- [ ] App size reasonable; no debug flags; Crashlytics mapping files uploaded.

## 3. Store listing
- [ ] **App name** (max 30 chars): e.g. "Sun Flowers - Flower Delivery".
- [ ] **Short description** (max 80): "Fresh flowers, bouquets and garlands delivered to your door in minutes."
- [ ] **Full description** (max 4000): honest, no keyword stuffing, no claims we cannot meet (delivery radius and hours stated).
- [ ] **App icon** 512x512 PNG (32-bit, <= 1 MB), no misleading badges.
- [ ] **Feature graphic** 1024x500.
- [ ] **Phone screenshots**: min 2, recommended 6–8 (16:9 or 9:16 within limits); show real UI, include Tamil screenshots (localised listing: English + Tamil).
- [ ] Optional promo video (YouTube).
- [ ] Category: Shopping (or Lifestyle). Tags relevant. Contact email, website, **privacy policy URL** (mandatory).
- [ ] No third-party brand infringement, no copyrighted images without licence, no fake awards/ratings.

## 4. App content section (all must be completed)
- [ ] **Privacy policy**: public HTTPS page, same language as app, covers data collected, purposes, sharing (Firebase, Cashfree, Google Maps), retention, deletion, contact/grievance officer.
- [ ] **App access**: provide a working reviewer login. Use a Firebase **test phone number + fixed OTP** and instructions; also explain Admin demo credentials if reviewers might need them (optional separate test admin number).
- [ ] **Ads**: declare "No ads".
- [ ] **Content rating** questionnaire (IARC): shopping app, no violence/UGC → typically Everyone/3+. Answer honestly.
- [ ] **Target audience**: 18+ or 13+; NOT designed for children (avoid Families policy obligations).
- [ ] **News app / COVID / Health / Government / Financial features** declarations: answer "No" where not applicable; payments for physical goods are not a financial service (verify wording on the Financial features form).
- [ ] **Data safety form** (must match actual behaviour and privacy policy):
  - Collected: Personal info (name, phone number, email if Google login, address), Location (approximate/precise – for delivery address), Financial info (purchase history/payment method type only; card data handled by Cashfree), App activity/in-app search, Device or other IDs (FCM token), Crash logs/diagnostics.
  - Purposes: app functionality, account management, fraud prevention/security, analytics (if enabled).
  - Shared with third parties: payment processor (Cashfree) and service providers (Firebase/Google) as applicable – declare accurately.
  - Data encrypted in transit: Yes. **Users can request deletion: Yes.**
- [ ] **Account deletion**: in-app deletion path AND a public web URL where users can request deletion (declare URL in Data safety > Data deletion). Mandatory for apps with account creation.
- [ ] **Permissions declarations**: location (foreground only, core functionality = choosing delivery address), notifications (runtime on Android 13+). No background location, no SMS/Call log permissions (use SMS Retriever / Firebase Auth), no `QUERY_ALL_PACKAGES`.
- [ ] **Advertising ID** declaration: declare usage (No if not used; Firebase Analytics may use it – check and answer accurately or disable ad-ID collection).
- [ ] **Government app**: No. **Health apps**: No.

## 5. Policy compliance
- [ ] **User Data policy**: transparent disclosure + consent before collecting personal data (our login consent screen). Prominent disclosure not required for foreground location, but explain why before the OS prompt.
- [ ] **Payments policy**: physical goods/services can use external payment (Cashfree/UPI/COD) – Play Billing not required. No digital goods sold in-app.
- [ ] **Deceptive behaviour / Metadata**: screenshots reflect real UI, description accurate, no misleading permissions.
- [ ] **Intellectual property**: own all images/fonts; licences recorded.
- [ ] **Restricted content**: none (no alcohol/tobacco/regulated goods). Do not sell anything regulated in the app.
- [ ] **Minimum functionality / app stability**: no crashes on launch, no ANRs, no broken links. Pre-launch report in Play Console reviewed and issues fixed.
- [ ] **Reviewer-friendly flow**: if the shop is closed at review time, the reviewer cannot place an order. Keep the shop OPEN (or provide a test mode) during review and clearly mention in review notes how to place a test COD order (and that real deliveries are within a service radius – provide a test address within radius or a staging "test location" flag controlled by test account).
- [ ] Deep links verified; no "coming soon" screens; no empty menu items.

## 6. Testing tracks and release
- [ ] Internal testing (up to 100 testers) → quick checks.
- [ ] Closed testing → 12+ testers x 14 days if required; collect feedback; fix crashes.
- [ ] Pre-launch report clean (accessibility, security, performance warnings triaged).
- [ ] Production: staged rollout (10% → 50% → 100%) over several days; monitor Crashlytics and Play Vitals.
- [ ] Release notes in English and Tamil.

## 7. After submission
- Review typically takes a few days (first apps can take longer). Respond to policy emails within the stated time.
- Rejection common causes: missing/incorrect privacy policy, Data safety mismatch, no account deletion URL, reviewer cannot log in, crashes on launch, misleading screenshots, wrong target API. Fix and resubmit with explanatory notes.
- Keep policy/Data safety in sync whenever features or SDKs change.
