# 04 – Design System and Screen Specs

Source of truth for visuals: the supplied mockup (splash, home, order details). Match it closely,
then extend the same language to all other screens. The app must feel custom-designed: consistent
spacing, real micro-interactions, no default-template look.

## 1. Brand
Name: **Sun Flowers and Decoration** · Tagline: *Flowers... Always with You...* · Logo: rose-in-diamond mark (supplied asset; request vector/SVG + 1024 PNG from owner).

## 2. Colour tokens
```ts
export const colors = {
  primary: '#E6197A',        // CTA, active tab, badges
  primaryDark: '#C20F64',    // pressed
  primarySoft: '#FCE4EE',    // chips, card tint, selected states
  bg: '#FFF7FA',             // app background
  surface: '#FFFFFF',
  navy: '#2B1B4B',           // headings, logo text
  text: '#2B1B4B',
  textMuted: '#6B6B7B',
  border: '#F1D9E4',
  success: '#2E9E5B', warning: '#F5A524', danger: '#D93025',
  overlay: 'rgba(43,27,75,0.45)',
};
```
Dark mode: V1 supports light only (document this); respect system font scale.
Contrast: primary on white >= 4.5:1 for text; verify with a contrast checker; use `primaryDark` for small text.

## 3. Typography
- Display/brand: **Playfair Display** (SemiBold) for logo text, hero taglines, screen titles on splash/home banner.
- UI: **Poppins** (Regular 400, Medium 500, SemiBold 600).
- Tamil: **Noto Sans Tamil** fallback for all Tamil strings (ensure good line-height 1.5).
- Scale: 12 / 14 / 16 / 18 / 20 / 24 / 32. Line height 1.4 (1.5 Tamil).
- Support OS font scaling up to 1.3x without clipping.

## 4. Spacing, shape, elevation
Spacing 4-8-12-16-20-24-32. Radius: card 16, button 14, input 12, chip/pill 999, image 12.
Shadow: soft, pink-tinted (`#E6197A` at 8–12% opacity, blur 16, y 6). Touch targets >= 48x48 dp.

## 5. Components (build once, reuse everywhere)
Button (primary/secondary/ghost/danger, loading, disabled), IconButton, TextInput (label, error, helper),
OtpInput (6 boxes, autofill), SearchBar, CategoryTile, ProductCard (grid), ProductListRow, PriceText (₹ formatting, strikethrough MRP, discount %),
QuantityStepper, Badge (cart count), Chip, BannerCarousel (dots), SectionHeader ("Our Special Picks" + View All),
StatusBanner (shop closed message), OrderStepper, OrderCard, AddressCard, EmptyState (illustration + CTA), ErrorState (retry),
SkeletonLoader, Toast/Snackbar, BottomSheet, ConfirmDialog, ListDivider, RatingStars (display only), Avatar, FAB.
Each component: typed props, accessibility label/role, tested in isolation.

## 6. Motion and feedback
- Splash: logo fade/scale-in 600 ms, petals subtle, loading indicator; max 1.5 s unless auth/bootstrapping not done.
- Add-to-cart: button morphs to stepper; cart badge bounces; light haptic.
- Wishlist heart: scale pop + haptic.
- Screen transitions: native stack defaults; list items fade-in staggered (<= 150 ms total) on first load only.
- Pull-to-refresh on lists. Skeletons instead of spinners for lists.
- Order stepper: animated progress line when status changes.
- Respect "Reduce motion" setting.

## 7. Navigation map
```
Splash → (Onboarding once) → Login (OTP/Google) → Profile name (first time) → Tabs
Tabs: Home | Track | Wishlist | Account
Home → Category list → Product detail → Cart → Address select/add → Checkout → Order success → Order details
Home hamburger (drawer): About us, Contact, Policies, Language, Share app
Account → Orders, Addresses, Language, Support, Legal pages, Delete account, Logout
Admin (shown only if role=admin; entry via Account → "Shop Admin" and a distinct tab/stack):
  Dashboard | Orders | Products | Settings (Shop status, Delivery, Partners, Banners, Categories)
```

## 8. Screen specs
**Splash**: gradient pink background, petals top-left and bottom roses (from asset), logo, tagline, loader, "Loading...". Checks session + `/shop/status` + remote config in parallel; routes accordingly.

**Login**: phone input (+91 prefilled), "Send OTP" → OTP screen (autofill via SMS Retriever, 30 s resend timer, error states), "Continue with Google". Terms/Privacy links with consent text ("By continuing you agree...").

**Home**: header (hamburger, logo, cart with badge). **StatusBanner** (only when shop closed: pink-red tinted card with admin message + reopen time). Banner carousel (auto-scroll 4 s, pause on touch). Category grid 3 x 2 (+ "More"). "Our Special Picks" horizontal/2-col grid with price and add (+) button. Bottom tabs as per mockup.

**Category/Search**: list/grid toggle optional, filters (in stock), sort (price), search with debounce 300 ms, empty state ("No flowers found").

**Product detail**: image gallery (pinch zoom, dots), name, price/MRP/discount, unit label, description, stock state, wishlist, sticky bottom bar: stepper + "Add to cart". When shop closed: button disabled with label "Shop closed" and banner on top; browsing still works.

**Cart**: items with steppers, remove (swipe/long-press with undo snackbar), bill summary (subtotal, delivery, taxes row only when tax > 0, total — values come from `/cart/validate`), min-order warning, closed-shop blocker, "Select address" CTA.

**Address**: list, add/edit with map (drag pin, current-location button, search), fields, live radius feedback ("2.4 km from shop – deliverable" / "Sorry, we deliver within 6 km only"). Location permission is requested only when tapping "Use current location" with a clear rationale.

**Checkout**: address summary, delivery ETA, payment method (COD / Online), note, bill, "Place Order" (disabled while submitting, idempotency key generated once per attempt).

**Order success**: animation, order number SF-xxxxxx, ETA, buttons "Track order", "Continue shopping".

**Order details / Track** (mockup screen 3): back, title, call-shop icon; stepper (Order Placed → Preparing → Out for Delivery → Delivered); map card (shop marker → destination marker, ETA chip "Arriving in 25 min" computed from placedAt + estimate while out_for_delivery); delivery partner card (name, rating optional/hidden in V1 unless real data, call and chat/WhatsApp buttons); Order Summary with items, delivery charges, taxes row only when tax > 0 (the mockup's Taxes & Fees line is hidden for this shop), total; "Contact Support" CTA; "Cancel order" visible only in `placed`.

**Track tab**: list of active orders (or EmptyState with CTA).

**Wishlist**: grid, remove, add to cart.

**Account**: avatar + name, My Orders, Addresses, Language, Help & Support, About, Privacy Policy, Terms, Refund Policy, Delete Account (red, confirm with typed word/re-auth), Logout, app version.

**Admin screens**
- Dashboard: cards (Today's orders, Revenue, Pending, Preparing), shop status card at top with big toggle.
- Shop Status screen: switch Open/Closed, message text area (300 char counter, quick templates: "Shop is closed today. We will reopen tomorrow at 8:00 AM." and Tamil equivalent), date-time picker for reopen, live preview of how users will see it, Save (confirm dialog when closing).
- Orders: tabs (New, Preparing, Out, Completed, Cancelled), card with number, items count, total, time, distance; detail with customer name/phone (call), address (open in Maps), status action buttons, assign partner, cancel with reason.
- Products: list with search, availability switch, create/edit form (images up to 5, compress client-side to <= 1200 px, WebP/JPEG 80%), preview.
- Categories, Banners, Partners CRUD. Delivery Settings form. Audit log (read-only).

## 9. State matrix (every data screen)
loading (skeleton) · empty (illustration + helpful CTA) · error (message + Retry) · offline (banner "No internet", cached data where possible) · success.

## 10. Copy and localisation
All strings in `en.json` and `ta.json`; Tamil reviewed by a native speaker. Currency `₹` with `Intl.NumberFormat('en-IN')`. Dates in `Asia/Kolkata`. Never concatenate translated fragments; use interpolation.

## 11. Assets checklist
App icon (adaptive: foreground 432x432 safe zone + background), notification icon (white monochrome), splash assets, category images, banner images, empty-state illustrations, store graphics (see Play Store checklist). Use original/licensed images only; keep a `LICENSES.md` for assets and fonts.
