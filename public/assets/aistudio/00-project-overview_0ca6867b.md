# 00 – Project Overview

## 1. Vision
A fast, trustworthy, beautiful flower delivery app for one shop (Sun Flowers and Decoration).
Customers in a 5–6 km radius order flowers, garlands, bouquets, arrangements and decorations
and receive them quickly. The owner controls availability from the same app.

## 2. Roles
| Role | Description | How created |
|---|---|---|
| Customer | Browses, orders, tracks | Self sign-up via Phone OTP / Google |
| Admin (Shop owner) | Manages shop status, products, orders, settings | Seeded manually in DB (never via API) |
| Delivery partner | Delivers orders | V1: just a record (name, phone, photo) managed by admin and shown to customer. No separate login in V1 |

## 3. V1 scope (must ship)

**Customer**
- Splash, onboarding (optional single screen), login (OTP/Google), profile completion (name)
- Home: banner carousel, category tiles, "Our Special Picks", search
- Category listing, product detail (images, price, description, stock)
- Wishlist, cart, address management with map pin, delivery-radius validation
- Checkout (COD + Cashfree online payment: UPI, cards, netbanking), order success, order history, order details with status stepper
- Order tracking screen (status stepper, ETA text, delivery partner name + call button, static map with shop and destination)
- Cancel order (only while status = placed), contact support (call/WhatsApp), reorder
- Account: profile, addresses, orders, language (Tamil/English), privacy policy, terms, refund policy, about, **delete account**
- Push notifications for order updates and shop announcements

**Admin**
- Dashboard (today's orders, revenue, pending count)
- **Shop status toggle (open/closed)** with **custom closed message** and optional **reopen date/time**
- Orders list with filters; accept, prepare, assign delivery partner, out for delivery, delivered, cancel (with reason)
- Product CRUD with image upload, stock toggle, featured toggle
- Category CRUD, banner CRUD
- Delivery settings: shop location, radius, delivery charge, minimum order, tax %, estimated delivery minutes
- Delivery partner CRUD
- Loud new-order notification

## 4. Out of scope for V1 (roadmap, Phase 12)
Live rider GPS tracking, separate delivery partner app, coupons/offers, ratings and reviews,
scheduled/advance delivery (weddings, festival), subscriptions, iOS release, multi-shop.

## 5. Business rules (authoritative)
1. Shop **closed** → customers can browse everything but cannot add to cart for checkout or place orders. Server rejects `POST /orders` with `SHOP_CLOSED`.
2. The closed message is fully admin-controlled (free text, Tamil or English). If `reopen_at` is set, the app also shows "Opens tomorrow 8:00 AM" style text in the user's language, and the shop auto-reopens at that time (computed server-side, no cron).
3. Delivery allowed only if road/aerial distance from shop <= `delivery_radius_km` (default 6). Checked on address save AND again on order placement.
4. All money stored as **integer paise** (₹499 = 49900). No floats.
5. Prices, delivery charge, tax and total are always computed on the server. Client values are ignored.
6. Order status machine (only these transitions):
   `placed → preparing → out_for_delivery → delivered`; `placed|preparing → cancelled`.
   Customer can cancel only in `placed`. Admin can cancel in `placed` or `preparing` with a reason.
7. Minimum order value is configurable. Delivery charge is configurable (can be free above a threshold).
8. Out-of-stock products cannot be added to the cart; the server re-checks stock at checkout.
9. A customer can have max N (default 5) saved addresses.
10. Account deletion: user data removed/anonymized; order records retained in anonymized form for accounting as legally required.
11. Timezone: store UTC (`timestamptz`), display `Asia/Kolkata`.
12. The shop is NOT registered under GST. `tax_percent` stays at **0**; the app must never charge, label or display GST/tax to customers while it is 0. If the shop later registers (or crosses the turnover threshold), the owner sets the percentage in Admin settings and the Taxes line appears automatically. No GSTIN is shown anywhere unless the owner adds one.

## 4b. Assumptions to confirm with the shop owner before Phase 5
- Exact shop GPS location, delivery radius (5 or 6 km), delivery charge slab
- Opening hours, minimum order, cancellation window (tax stays 0: shop is not GST registered)
- Customer support phone/WhatsApp, business address, grievance contact (GSTIN not applicable)
- Cashfree account (needs business KYC) – can launch COD-only first
- Real product photos and descriptions (do not use stock/AI images of products you cannot deliver)

## 6. Success metrics
Crash-free sessions > 99.5%; order placement success > 98%; cold start < 3 s on mid-range device;
API p95 < 400 ms; Play Store approval on first review.
