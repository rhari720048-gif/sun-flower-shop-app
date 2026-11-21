# Phase 05 – Shop Status, Addresses, Delivery Radius

**Duration:** 5 days · **Depends on:** Phase 04 (admin UI for toggling is built in Phase 7; backend + customer side here)

## Goal
Implement the owner's key requirement: when the shop is closed, users can view products but cannot place orders, and see the admin's custom message. Implement address management with strict 5–6 km radius enforcement.

## Backend tasks
### Shop status
- [ ] `PATCH /admin/shop` (admin only): validates fields, message <= 300 chars, default message auto-filled if closing without message, `reopenAt` must be in the future, audit log entry, cache invalidation
- [ ] On change send FCM **data** message to topic `all_customers`: `{type:'shop_status'}`; customers subscribe to the topic at login, unsubscribe at logout
- [ ] Shared guard `assertShopOpen()` used by `/cart/validate` (returns flag) and `POST /orders` (throws `SHOP_CLOSED` with message + reopenAt in `details`)
### Addresses and radius
- [ ] `geo.ts`: Haversine, `isWithinRadius`, unit tests with known coordinates (boundary at exactly the radius, 0 km, antipodal sanity)
- [ ] Address CRUD with server-side `distance_km` computation and `OUT_OF_RADIUS` rejection; max addresses per user; one default address
- [ ] Reverse geocoding / place search proxy **only if needed** (prefer client-side Google Places with restricted key; if proxied, cache and rate limit)
- [ ] Integration tests: boundary cases, tampered distance field ignored, changing radius setting affects new validations, ownership/IDOR

## Mobile tasks
### Shop status
- [ ] `useShopStatus()` hook: TanStack Query with `refetchInterval` 45 s on Home/Cart/Product, refetch on app foreground and on FCM data message
- [ ] **StatusBanner** on Home, Cart, Product detail: shows admin message (Tamil/English as typed) and localized "Opens on <date> at 8:00 AM" when `reopenAt` exists; subtle animation on appear
- [ ] Product detail: Add-to-cart disabled with "Shop closed" label; Home product "+" buttons disabled; wishlist still allowed
- [ ] Clock-skew safe: use `serverTime` offset for countdown/text; never rely on device time for open/closed decisions
### Addresses
- [ ] **Address list**, **Add/Edit** with map (react-native-maps), draggable pin, "Use current location" (just-in-time permission with rationale, graceful denial), search by place
- [ ] Live radius feedback as pin moves (client-side Haversine for UX only; server is authoritative) with clear messages
- [ ] Pincode/phone validation (6-digit, Indian mobile), landmark field, default address, delete with confirm
- [ ] Location permission denied/permanently denied flows (open settings)

## Tests
- [ ] Unit: haversine, effective open, banner text (all combos, both languages)
- [ ] Maestro: (staging) admin toggles closed via API → customer app shows banner and disabled CTA; reopen → restored
- [ ] Manual: pin at 5.9 km accepted / 6.1 km rejected with real coordinates around the shop

## Security checks
- Location only requested when needed, foreground only; coordinates not logged; admin PATCH requires revoked-check; audit entries verified

## Acceptance criteria
- [ ] Closed shop: browsing works, ordering impossible both in UI and via direct API call (`SHOP_CLOSED`)
- [ ] Custom message and reopen time appear in the customer app within 60 s (instantly with push) in both languages
- [ ] `reopen_at` passing flips the shop open without any manual action or cron
- [ ] Out-of-radius addresses cannot be saved; in-radius addresses show distance
- [ ] Permission flows tested on Android 8–14

## Pitfalls
- Trusting device clock; hard-coding 8 AM text instead of using admin's `reopenAt`/message; using aerial distance only without telling the owner (document that it is straight-line distance; optionally upgrade to Distance Matrix)
