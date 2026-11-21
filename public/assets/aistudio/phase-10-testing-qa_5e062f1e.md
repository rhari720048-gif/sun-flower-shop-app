# Phase 10 – Testing and QA

**Duration:** 6–7 days · **Depends on:** Phase 09 · **Read first:** 07-testing-strategy

## Goal
Prove the app works, fails safely, and is ready for real customers. No known P0/P1 bugs.

## Tasks
### Automated
- [ ] Backend unit + integration suites complete and green in CI (coverage reports archived); includes every test case from 07 §"Critical test cases"
- [ ] Mobile unit/component tests green; Maestro suites for: auth, browse + wishlist, cart + COD order, closed shop blocking, out-of-radius, admin toggle, admin order handling, account deletion, language switch
- [ ] Contract check: OpenAPI vs shared Zod schemas; mobile parses responses with Zod (no silent shape drift)
- [ ] Load test (k6) and soak test 30 min; no memory leaks in backend (monitor RSS)

### Manual (use the QA sheet below; track in a spreadsheet/issue tracker)
- [ ] Device/OS matrix from 07 (>= 6 devices incl. low-RAM and aggressive-battery brands)
- [ ] Network conditions: offline, airplane toggle mid-order, 3G throttle, captive/slow Wi-Fi, switching Wi-Fi ↔ mobile data
- [ ] Interruptions: incoming call, app backgrounded during payment, kill and relaunch, low storage, battery saver, rotate (portrait locked), split screen
- [ ] Time: change device clock (shop status must not rely on it), IST vs other timezone device setting
- [ ] Real-world pilot: owner + 5–10 friends/family place real COD orders in the real radius using the staging-to-production candidate (production backend with test-flag customers allowed) to validate operations end to end (packing, delivery time, notification clarity)

### QA sheet (minimum)
| Area | Scenario | Expected |
|---|---|---|
| Auth | OTP wrong/expired/resend/limits | Specific localized errors, no crash |
| Auth | Google sign-in cancel/back | Returns to login cleanly |
| Shop | Closed + message + reopen time | Banner shown, ordering blocked, browse OK |
| Shop | Reopen by time | Auto open, banner disappears |
| Radius | 5.9 / 6.0 / 6.1 km | Accept / accept / reject |
| Cart | Qty limits, stock change while in cart | Handled with message |
| Order | Double tap / retry / network drop | Exactly one order |
| Payment | Success / fail / cancel / kill app | Consistent state |
| Status | Each transition | Correct push + UI update |
| Admin | All CRUD, upload large photo | Works, compressed, audited |
| Delete | Account deletion + re-signup | Clean, no residual data |
| i18n | Full Tamil walkthrough | No clipping/missing keys |
| A11y | TalkBack checkout | Completable |
| Security | Items in 06 §10 | All pass |

### Bug triage
P0 crash/data loss/payment or security issue → fix now · P1 core flow broken → fix before release · P2 minor UI/logic → fix or schedule · P3 cosmetic → backlog.

## Exit criteria
- [ ] 0 open P0/P1; P2 list reviewed and accepted by owner
- [ ] All automated suites green on release candidate build; crash-free sessions >= 99.5% in internal testing
- [ ] Pilot orders delivered successfully with timing within estimate; owner signs off
- [ ] Release candidate AAB built via CI from a tagged commit (reproducible)

## Deliverables
`docs/qa-report.md` (results, device matrix, known issues), regression checklist for future releases, tagged release candidate `v1.0.0-rc1`.
