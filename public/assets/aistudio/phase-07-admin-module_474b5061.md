# Phase 07 – Admin Module

**Duration:** 7–8 days · **Depends on:** Phase 06

## Goal
The shop owner runs the whole business from the app: shop open/close with custom message, order handling, catalogue management and settings.

## Backend tasks
- [ ] `GET /admin/dashboard`: today's orders, revenue (delivered, paise), pending/preparing counts, last 7 days series (timezone Asia/Kolkata day boundaries)
- [ ] `GET /admin/orders` (status/date/search filters, cursor) and `GET /admin/orders/:id`
- [ ] `PATCH /admin/orders/:id/status` with state machine, partner assignment, cancel reason, localized customer push, COD `paid` marking on delivery, refund flag for paid-online cancellations
- [ ] CRUD: `/admin/products`, `/admin/categories`, `/admin/banners`, `/admin/delivery-partners` (soft delete when referenced, validation of Tamil+English names, price rules)
- [ ] `POST /admin/uploads/sign`: signed Firebase Storage upload (mime/size limits, random path `products/<uuid>.webp`); Storage rules: public read for product/banner paths, write only via signed URL/admin
- [ ] `PATCH /admin/shop` finalised (Phase 5) + settings validation (radius 1–25 km, charges >= 0, tax 0–28% (default 0; show a warning in the form that tax should be set only if the shop is GST registered))
- [ ] `GET /admin/audit-log`; audit entries for every admin write (diff of changed fields)
- [ ] Admin FCM: high-priority "new order" push to all admin device tokens; notification channel `new_orders` with sound
- [ ] Tests: every admin route returns 403 for customers; audit log written; invalid transitions rejected; upload constraints enforced

## Mobile tasks
Admin area appears only when `role = admin` (Account → "Shop Admin"; consider a separate bottom-tab set). Route guard checks role from server session, not local flags.
- [ ] **Dashboard**: stat cards, 7-day chart, big shop status card with quick toggle
- [ ] **Shop Status screen**: switch, message textarea (300 chars counter), quick templates (EN/TA), reopen date-time picker (IST), live preview exactly as customers see it, confirm dialog on closing/opening, success toast; shows "Last changed by/at"
- [ ] **Orders**: segmented tabs (New / Preparing / Out / Completed / Cancelled), cards with number, items, total, distance, age; pull-to-refresh; 15 s polling when focused; sound/vibration on new order while app is open
- [ ] **Order detail (admin)**: customer name + call button, address + "Open in Maps", items, notes, payment info, action buttons by state (Accept → Preparing, Assign partner → Out for delivery, Delivered, Cancel with reason sheet), history timeline
- [ ] **Products**: list/search, availability switch (instant), create/edit form (en+ta names/descriptions, price, MRP, unit label, category, featured, up to 5 images with client-side compression + progress), delete/deactivate confirm
- [ ] **Categories**, **Banners** (image + target), **Delivery partners** CRUD
- [ ] **Delivery settings**: shop location picker (map pin), radius, charge, free-delivery threshold, min order, tax %, ETA minutes, support phone/WhatsApp, payment toggles
- [ ] **Audit log** screen (read-only)
- [ ] Optimistic UI only where safe (availability toggle); order status changes wait for server confirmation

## Tests
- [ ] Component tests for forms (validation, char counter); Maestro admin flow: toggle closed with message → customer device sees banner → reopen
- [ ] Manual: admin receives audible push for a new order with app in background and killed

## Security checks
- Admin routes protected server-side (never rely on hidden UI); revoked-token check; rate limits; audit trail complete; uploads restricted; admin list responses exclude unnecessary customer PII (phone only where needed to call)

## Acceptance criteria
- [ ] Owner can, without developer help: close the shop with a Tamil/English message and reopen time, handle an order end to end, add a product with photos, change delivery radius/charge
- [ ] Customer app reflects each admin change within the stated latency
- [ ] Every admin action appears in the audit log
- [ ] Non-admin user cannot reach or call any admin function (tested via UI and direct API)
- [ ] Owner usability review done in Tamil UI; text sizes readable

## Pitfalls
- Hiding admin UI but not protecting API; huge uncompressed images from phone cameras; push not arriving on aggressive-battery phones (document "disable battery optimisation for Sun Flowers" in admin onboarding); forgetting IST day boundaries in dashboard queries
