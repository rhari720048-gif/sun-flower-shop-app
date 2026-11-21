# 07 – Testing Strategy

## Pyramid
| Level | Tool | Scope | Target |
|---|---|---|---|
| Unit (backend) | Vitest/Jest | pricing, geo/haversine, state machine, shop effective-open, idempotency, validators | >= 90% of these modules |
| Integration (backend) | Vitest + supertest + Postgres (Neon branch or container) | every endpoint incl. auth/role/IDOR, transactions, webhooks | all endpoints, happy + failure |
| Unit/Component (mobile) | Jest + React Native Testing Library | components, hooks, stores, formatters, i18n | critical flows >= 70% |
| E2E (mobile) | Maestro (flows in YAML) | login, browse, add to cart, address, checkout COD, closed-shop blocking, admin toggle | all P0 flows |
| Manual QA | Real devices | device/OS matrix, network conditions, Tamil UI | checklist below |
| Security | see 06-security.md | | all pass |

## Test data
Dedicated staging Firebase project with **Firebase test phone numbers** (fixed OTP) for automation and for Play review credentials.
Seed script creates categories, 20+ real-looking products, one admin, one customer.

## Critical test cases (must exist)
**Shop status**
1. Admin toggles OFF with message → customer app shows banner within 60 s (or instantly via push); cart checkout button disabled; browsing works.
2. `POST /orders` while closed → `SHOP_CLOSED` even with modified client.
3. `reopen_at` in the past → effective open without cron; `reopen_at` in future → still closed.
4. Message with 301 chars rejected; Tamil text saved and displayed correctly; empty message → default text.

**Radius**
5. Address at 5.9 km accepted, 6.1 km rejected (radius 6). Boundary exactly = radius accepted.
6. Tampered lat/lng in `POST /orders` ignored (uses stored address). Changing shop radius affects new orders immediately.

**Orders**
7. Price tamper attempt (client sends different price) ignored.
8. Duplicate submit with same Idempotency-Key returns same order, one DB row.
9. Out-of-stock/inactive product → `OUT_OF_STOCK`. Below min order → `BELOW_MIN_ORDER`.
10. Status machine: every illegal transition rejected; customer cancel only at `placed`.
11. User A cannot GET/cancel user B's order (403/404).
12. Concurrent order placement (10 parallel) – no corruption.

**Payments**
13. Valid signature marks paid; invalid rejected; webhook replay ignored; failed payment leaves order `pending` and user can retry/cancel.

**Auth**
14. Expired/forged/no token → 401. Customer token on admin route → 403. Revoked admin token rejected.
15. Account deletion removes PII, Firebase user, device tokens; orders anonymised; re-login creates fresh account.

**Mobile**
16. Offline: cached catalogue visible, banner shown, order blocked with clear message, retry works.
17. App killed and reopened mid-order: cart restored, no duplicate order.
18. Push received in foreground, background, killed; tapping opens the right order.
19. Back button behaviour, rotation (lock portrait), font scale 1.3, TalkBack basics.
20. Language switch updates all screens without restart.

## Device/OS matrix
Android 8, 10, 12, 13, 14, latest; small (5") and large screens; at least one low-RAM (2 GB) device; slow 3G throttled test; Samsung, Xiaomi/Redmi, Realme/Oppo (aggressive battery managers affect push – test).

## Performance budgets
Cold start < 3 s mid-range; list scroll 60 fps (no jank on 50 items); API p95 < 400 ms; release AAB < 40 MB; image payloads per screen < 1.5 MB.

## Exit criteria for release
Zero open P0/P1 bugs, all P0 E2E green, security checklist complete, crash-free >= 99.5% in closed testing, accessibility pass, Tamil review signed off.
