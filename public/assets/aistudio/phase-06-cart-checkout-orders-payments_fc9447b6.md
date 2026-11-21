# Phase 06 – Cart, Checkout, Orders, Payments

**Duration:** 7–8 days · **Depends on:** Phase 05

## Goal
Customers can build a cart, choose an address, pay (COD or Cashfree online), place an order safely, and view their order history/details.

## Backend tasks
### Pricing and validation
- [ ] `pricing.ts` (pure, tested): subtotal, delivery charge (free-above rule), tax (0 by default, shop not GST registered), total — integer paise only
- [ ] `POST /cart/validate`: re-reads products (active, in stock, current price), shop open flag, min order, radius (if `addressId`), returns line items + totals + blocking reasons list
### Orders
- [ ] `POST /orders` (Idempotency-Key required): transaction → re-validate everything → insert order + items (snapshots) + status history `placed` → commit → post-commit notifications to admins
- [ ] Order number display format `SF-000123`
- [ ] `GET /orders`, `GET /orders/:id` (ownership, includes items, history, partner, shop + destination coordinates)
- [ ] `POST /orders/:id/cancel` (only `placed`; pushes to admin; online-paid → `refund_pending`)
- [ ] State machine module with exhaustive tests
### Payments (Cashfree)
- [ ] `lib/cashfree.ts`: thin typed client over the Cashfree PG REST API (or official Node SDK) with env `CASHFREE_ENV` (SANDBOX/PRODUCTION → `https://sandbox.cashfree.com/pg` or `https://api.cashfree.com/pg`), pinned `x-api-version`, timeouts, retries only on idempotent GETs
- [ ] Online order: after creating our order, call Create Order with `order_id` (our order id, alphanumeric/underscore/hyphen), `order_amount` (rupees string from paise via integer math), `order_currency: INR`, `customer_details` (stable `customer_id` = our user id, 10-digit `customer_phone`, name), `order_meta.return_url`/notify url, `order_expiry_time`. Store `cashfree_order_id`, `payment_expires_at`; return `payment_session_id` + `order_id` to the app
- [ ] `POST /payments/verify {orderId}`: backend fetches order/payment status from Cashfree; mark `paid` only if PAID and amount matches; idempotent
- [ ] `POST /payments/webhook`: raw-body signature verification (`x-webhook-signature`, `x-webhook-timestamp`), idempotent by `event_id`, handles success/failed/user-dropped/refund events, logs to `payment_events`, acknowledges 200 quickly
- [ ] Refunds: admin cancel of a paid online order triggers Cashfree Create Refund API (idempotent `refund_id`), status updated by refund webhook; `refund_pending` → `refunded`
- [ ] Unpaid online orders auto-cancel at `payment_expires_at` (computed on read + lightweight scheduled sweep; document the choice) so they never reach the shop unpaid
- [ ] COD: `payment_status = pending` until delivered, then admin marks `paid`
- [ ] Feature flags: `cod_enabled`, `online_payment_enabled` from shop settings (launch can be COD-only until Cashfree is activated)

## Mobile tasks
- [ ] Cart store (Zustand + AsyncStorage persistence): add/update/remove, max qty 50, merge duplicates, price display from server validate (local prices are display hints only)
- [ ] **Cart screen**: steppers, remove with undo, bill summary from `/cart/validate`, blockers (closed/min order/out of stock) shown clearly with fix actions
- [ ] **Checkout**: address selection (with add), ETA, payment method, note, "Place Order" with single-flight guard; generate idempotency key per attempt and reuse on retry
- [ ] Cashfree SDK integration: `react-native-cashfree-pg-sdk` (use current stable version per Cashfree docs; requires a dev/production build, not Expo Go). Build `CFSession(payment_session_id, order_id, CFEnvironment)`, register `CFPaymentGatewayService.setCallback({ onVerify, onError })` on mount and remove it on unmount, start web checkout (`doWebPayment`) and/or UPI intent. Treat `onVerify` only as a signal to call `POST /payments/verify`; handle failure, user cancel, app killed during payment (on next launch reconcile via `GET /orders/:id`). Add the SDK's Android/iOS setup (config plugin or prebuild) and verify the release build still passes R8
- [ ] **Order success** animation; **Orders list** (Track tab shows active, Account > My Orders shows all); **Order details** per mockup with summary; reorder button
- [ ] Cancel order UI with confirmation
- [ ] Clear cart only after server confirms order

## Tests
- [ ] Unit: pricing (many cases incl. rounding, free-delivery threshold), state machine
- [ ] Integration: every rule in 07-testing-strategy cases 7–13, concurrency test (10 parallel same key → 1 order; 10 parallel different keys → 10 orders)
- [ ] Payment: forged signature, replayed webhook, amount mismatch, late webhook after cancel
- [ ] Maestro: add to cart → address → COD order → appears in orders → cancel

## Security checks
- Price/amount never accepted from client; ownership on all order reads; webhook secret separate; Cashfree client secret absent from app bundle (grep the APK)

## Acceptance criteria
- [ ] Double-tap Place Order or flaky network never creates duplicate orders
- [ ] Closed shop/out-of-radius/out-of-stock/below-min all blocked with specific localized messages
- [ ] Online payment success, failure, cancel and kill-app scenarios all end in a consistent order/payment state
- [ ] Totals identical on cart, checkout, success, details, admin view (single server source)
- [ ] Cashfree SANDBOX payments (UPI, card, netbanking test credentials) complete end to end; production checklist prepared (merchant activation/KYC, production keys, webhook URL registered, allowed domains/app package)

## Pitfalls
- Using floats for money; marking paid on client success callback; forgetting to snapshot product name/price in `order_items`; clearing cart before order confirmation; not handling webhook arriving before verify call; sending paise instead of rupees to Cashfree (amount is in rupees); parsing JSON before verifying the webhook signature
