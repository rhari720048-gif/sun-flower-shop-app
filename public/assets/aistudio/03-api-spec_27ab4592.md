# 03 – API Specification

Base URL: `https://api.<domain>/v1`. JSON only. All times ISO-8601 UTC. All money in paise (integers).

## Common
Headers: `Authorization: Bearer <firebase_id_token>` (except public routes), `X-Firebase-AppCheck: <token>`,
`Accept-Language: en|ta`, `X-Request-Id` (optional; server generates).

Error format (always):
```json
{ "error": { "code": "SHOP_CLOSED", "message": "Shop is closed right now", "details": {}, "requestId": "..." } }
```
Never return stack traces or SQL errors. Standard codes:
`UNAUTHENTICATED(401) FORBIDDEN(403) NOT_FOUND(404) VALIDATION_ERROR(422) RATE_LIMITED(429)
SHOP_CLOSED(409) OUT_OF_RADIUS(409) OUT_OF_STOCK(409) BELOW_MIN_ORDER(409) INVALID_STATE(409)
DUPLICATE(409) PAYMENT_FAILED(402) INTERNAL(500)`.

Pagination: cursor based `?limit=20&cursor=<opaque>` → `{ items: [], nextCursor: string|null }`. Max limit 50.

Rate limits (per IP + per user): global 120/min; `POST /orders` 10/min; admin writes 60/min; webhook route exempt but signature verified.

## Public
| Method | Path | Notes |
|---|---|---|
| GET | `/health` | `{status:'ok', db:'ok'}` |
| GET | `/shop/status` | `{isOpen(effective), message, reopenAt, estimatedDeliveryMinutes, deliveryRadiusKm, deliveryChargePaise, freeDeliveryAbovePaise, minOrderPaise, supportPhone, supportWhatsapp, codEnabled, onlinePaymentEnabled, serverTime}` – cached 15 s |
| GET | `/banners` | active, ordered |
| GET | `/categories` | active, ordered, localized by `Accept-Language` |
| GET | `/products?categoryId&featured&q&limit&cursor` | active only; `q` uses full-text/ILIKE |
| GET | `/products/:id` | 404 if inactive |
| GET | `/legal` | urls for privacy/terms/refund/shipping |

## Customer (auth required)
| Method | Path | Notes |
|---|---|---|
| POST | `/auth/session` | After Firebase login: upserts user, returns profile `{id,name,phone,role,language}` |
| GET/PATCH | `/me` | PATCH `{name, language}` only (whitelist fields) |
| DELETE | `/me` | Account deletion (see schema retention). Requires recent login (re-auth) |
| POST | `/me/device-token` | `{fcmToken}` upsert; DELETE on logout |
| GET/POST | `/addresses` | POST computes `distance_km`; if > radius returns `OUT_OF_RADIUS` (still allow saving? **No**: reject) |
| PATCH/DELETE | `/addresses/:id` | owner only |
| GET | `/wishlist` | products |
| PUT/DELETE | `/wishlist/:productId` | idempotent |
| POST | `/cart/validate` | Body `{items:[{productId,qty}], addressId?}` → server prices, stock, shop state, totals preview |
| POST | `/orders` | Header `Idempotency-Key` (uuid, required). Body `{items, addressId, paymentMethod, note?}`. Validates: shop effective-open, radius (re-computed from address), stock/active, min order, payment method enabled. Returns order (+ `cashfree: {paymentSessionId, orderId, environment}` if online) |
| GET | `/orders?limit&cursor` | own orders |
| GET | `/orders/:id` | own only (IDOR check) incl. items, history, deliveryPartner{name,phone,photo}, shop location, destination |
| POST | `/orders/:id/cancel` | only if `placed`; body `{reason?}` |
| POST | `/payments/verify` | `{orderId}` → backend calls Cashfree **Get Order** API itself and trusts only that result (`order_status = PAID` → mark paid). The client callback is only a hint to trigger this call |
| POST | `/payments/webhook` | Cashfree webhook. Verify `x-webhook-signature` = Base64(HMAC-SHA256(`x-webhook-timestamp` + rawBody, client secret)) using the **raw body**; reject stale timestamps; idempotent via `payment_events.event_id`. Handle payment success/failed/user-dropped and refund status events |

## Admin (role = admin, `checkRevoked`)
| Method | Path | Notes |
|---|---|---|
| GET | `/admin/dashboard` | today's counts, revenue (delivered), pending, last 7 days |
| GET/PATCH | `/admin/shop` | PATCH any of `{isOpen, closedMessage, reopenAt, shopLat, shopLng, deliveryRadiusKm, deliveryChargePaise, freeDeliveryAbovePaise, minOrderPaise, taxPercent, estimatedDeliveryMinutes, supportPhone, supportWhatsapp, codEnabled, onlinePaymentEnabled}`. Validate: message <= 300 chars; if `isOpen=false` and no message, server fills a default. On change → FCM data push `type=shop_status` to topic `all_customers`; audit log |
| GET | `/admin/orders?status&date&q&cursor` | all orders |
| GET | `/admin/orders/:id` | |
| PATCH | `/admin/orders/:id/status` | `{status, note?, deliveryPartnerId?, cancelReason?}` enforces state machine; out_for_delivery requires partner; cancel requires reason; pushes FCM to customer; if online & paid & cancelled → `refund_pending` |
| CRUD | `/admin/products`, `/admin/categories`, `/admin/banners`, `/admin/delivery-partners` | soft delete (`is_active=false`) when referenced |
| POST | `/admin/uploads/sign` | returns a signed upload URL/path for Firebase Storage; validate mime (jpeg/png/webp), max 3 MB |
| GET | `/admin/audit-log` | paginated |

## Business logic details
**Pricing** (`services/pricing.ts`): `subtotal = Σ unit_price × qty`; `delivery = subtotal >= freeAbove ? 0 : charge`;
`tax = round(subtotal × tax%)` (tax% is 0 for this shop, so tax = 0 and clients hide the row); `total = subtotal + delivery + tax`. All integer arithmetic.

**Distance**: Haversine (aerial) between shop and address. Return `distance_km` rounded to 2 decimals.
Optionally refine with Google Distance Matrix later. Radius check uses `<=`.

**Idempotency**: unique `(user_id, idempotency_key)`; a repeat request returns the original order (200) instead of creating a new one.

**Order creation transaction**: re-read shop settings → validate → re-read products → insert order, items, first history row → commit → (after commit) send admin push.

**Status change push copy** (localized en/ta): placed, preparing, out for delivery (with partner name), delivered, cancelled (with reason).

## OpenAPI
Generate OpenAPI from Zod schemas (`fastify-type-provider-zod` + `@fastify/swagger`), served only in non-production.
