# Phase 08 – Notifications, Tracking, Account, Support

**Duration:** 5 days · **Depends on:** Phase 07

## Goal
Complete the customer experience: push notifications, order tracking screen per mockup, full account area including **account deletion**, support and legal pages.

## Notifications
- [ ] Android 13+ `POST_NOTIFICATIONS` just-in-time prompt (after first order or in a pre-prompt screen) with graceful denial
- [ ] Notification channels: `orders` (default/high), `announcements` (default), admin `new_orders` (high, sound)
- [ ] Device token lifecycle: register after login, refresh on `onTokenRefresh`, delete on logout, backend prunes invalid tokens on FCM errors
- [ ] Templates localised (en/ta) per user language: order placed/preparing/out for delivery (partner name)/delivered/cancelled(reason); shop status announcements (optional opt-out)
- [ ] Deep link payload (`orderId`) → tapping opens Order details whether app is foreground, background or killed
- [ ] Monochrome notification icon, accent colour; grouping for multiple orders
- [ ] Topic `all_customers` subscribe/unsubscribe

## Tracking (V1 scope)
- [ ] Order details/Track screen exactly as mockup 3: animated stepper (Placed → Preparing → Out for Delivery → Delivered), map card with shop and destination markers and a straight route line, ETA chip ("Arriving in 25 min") computed from `outForDeliveryAt + estimate`, partner card (name, call, WhatsApp), summary, Contact Support
- [ ] Poll every 15 s while focused; stop on blur; update instantly on push
- [ ] Be honest in UI: no fake moving rider. (Live GPS tracking is a Phase 12 feature; the mockup scooter illustration is decorative only – use a static vehicle icon on the route line in V1.)
- [ ] Delivered state: thank-you card, reorder button

## Account and support
- [ ] Profile edit (name, language)
- [ ] **Delete account**: explanation of what is deleted/retained → re-authentication (OTP/Google) → typed confirmation → `DELETE /me` → sign out → confirmation screen. Matches Play requirement and public deletion web page
- [ ] Help & Support: call shop (`tel:`), WhatsApp deep link (`https://wa.me/<number>`), email; FAQ (cancellation, delivery area/time, payments)
- [ ] About, Terms, Privacy, Refund, Shipping pages (WebView of hosted pages or in-app browser); versions from `/legal`
- [ ] Share app link; rate-us prompt (Play In-App Review) after 3rd delivered order, once
- [ ] App version display, Play In-App Update (flexible) + forced-update screen for `minSupportedVersion`

## Tests
- [ ] Unit: notification payload handling/routing; Maestro: delete account flow on staging, re-signup works
- [ ] Manual: pushes in foreground/background/killed on Samsung, Xiaomi, Realme devices; Tamil text in notifications

## Security checks
- Notification payloads contain no PII beyond first name/order number; token deletion on logout; deletion requires fresh authentication; `tel:`/`wa.me` links use validated numbers from settings only

## Acceptance criteria
- [ ] All order status changes produce a correct localised push and deep link
- [ ] Account deletion fully removes PII (verified in DB and Firebase) and the public deletion page exists
- [ ] Tracking screen faithful to mockup, no misleading live-location claims
- [ ] Legal pages reachable from login, account and drawer
- [ ] Denied notification permission does not break flows; in-app status still updates via polling

## Pitfalls
- Data-only vs notification messages behaving differently when killed (use notification+data for order updates, data-only for silent shop-status refresh); forgetting channel setup on Android 8+; deletion that leaves Firebase user behind
