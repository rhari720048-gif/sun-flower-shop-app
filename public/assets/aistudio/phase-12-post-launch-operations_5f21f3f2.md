# Phase 12 – Post-Launch Operations and Roadmap

**Duration:** ongoing · **Depends on:** Phase 11

## Week 1–2 (stabilise)
- [ ] Daily review: Crashlytics, Play Vitals (crash rate, ANR rate), Sentry, uptime, failed payments, push delivery
- [ ] Respond to Play reviews within 48 h (English/Tamil); collect support issues in a tracker
- [ ] Hotfix process: branch from tag → fix → test → build → staged rollout; target < 24 h for P0
- [ ] Verify backups running; verify cost dashboards (Neon, hosting, Maps, SMS/Auth, FCM) and billing alerts

## Operations
- Weekly: dependency updates (Dependabot PRs), `pnpm audit`, review audit log anomalies
- Monthly: restore test of DB backup, secret rotation check, review Play policy updates and Data Safety accuracy, check target-API deadline and Expo SDK upgrade plan
- Quarterly: penetration-test checklist rerun, performance review, accessibility spot check
- Yearly: renew domain/certs (automatic), rotate keys, re-verify legal pages with advisor
- Always: bump `versionCode`; keep `CHANGELOG.md` and release notes; keep the QA regression checklist updated

## Owner runbook (Tamil + English short guide)
Open/close shop + message; handle new order; assign delivery partner; add/disable product; change radius/charge; what to do if push is not ringing (battery settings), if internet is down, if customer disputes an order, how to contact the developer.

## Roadmap candidates (prioritise with real data)
| Feature | Notes |
|---|---|
| Live rider tracking | Delivery partner app/screen sending GPS (foreground service + disclosure), SSE/WebSocket, Play background-location declaration if needed |
| Coupons / offers / festival banners | New tables, validation server-side, admin UI |
| Scheduled / advance delivery | Time slots, wedding/event decoration enquiry flow |
| Ratings and reviews | Moderation tools, Play UGC policy considerations |
| Decoration enquiry/quote requests | Photo upload, chat/WhatsApp handoff |
| Subscriptions (daily pooja flowers) | Recurring orders, reminders, pause/resume |
| Multiple delivery partners with login | Role `delivery`, assignment, earnings |
| Road-distance radius (Distance Matrix) | Replace aerial distance; cost control |
| iOS release | Apple developer account, privacy labels, review |
| Admin web dashboard | Reports, exports for accounting |

## Release management
Semantic versioning; every release: changelog, regression checklist, staged rollout, rollback plan (halt rollout, previous AAB stays available; backend backward compatible for at least 2 app versions; use `minSupportedVersion` carefully).

## Exit/health indicators
Crash-free >= 99.5%, ANR < 0.47%, order success >= 98%, average rating >= 4.2, support response < 1 h in shop hours.
