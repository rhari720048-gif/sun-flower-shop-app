# Phase 04 – Catalog, Search, Wishlist

**Duration:** 5 days · **Depends on:** Phase 03

## Goal
Customers can browse everything (even when the shop is closed): banners, categories, special picks, product lists, details, search, wishlist.

## Backend tasks
- [ ] `GET /banners`, `/categories`, `/products` (filters: category, featured, q, inStock; cursor pagination), `/products/:id`
- [ ] Localised fields chosen by `Accept-Language` (fallback to English if Tamil missing)
- [ ] Full-text search (`to_tsvector('simple')` + trigram/ILIKE fallback for Tamil), query sanitised, min 2 chars
- [ ] Wishlist: `GET /wishlist`, `PUT/DELETE /wishlist/:productId` (idempotent, ownership enforced)
- [ ] Cache headers (ETag, `Cache-Control: public, max-age=60`) for catalogue endpoints
- [ ] Seed realistic catalogue for dev/staging: >= 5 categories, 25+ products with real image URLs hosted in Storage
- [ ] Integration tests: inactive products hidden, pagination stable, search relevance, wishlist ownership, localisation fallback

## Mobile tasks
- [ ] **Home**: header (hamburger/logo/cart badge), StatusBanner slot (wired in Phase 5), banner carousel, category grid 3x2 + More, "Our Special Picks" with View All
- [ ] **Category list** and **product grid** with skeletons, pagination (infinite scroll), pull-to-refresh, sort/filter sheet
- [ ] **Search** screen with debounce, recent searches (local), empty state
- [ ] **Product detail**: gallery with zoom, price/MRP/discount, unit label, description, stock state, wishlist heart, sticky add-to-cart bar (cart logic in Phase 6; button wired to cart store now)
- [ ] **Wishlist tab** with optimistic updates and rollback on failure
- [ ] Image pipeline: `expo-image` caching + blurhash placeholders; admin uploads resized to <= 1200 px; thumbnails for lists
- [ ] Offline: persist catalogue queries (TanStack Query persister) for graceful offline browsing
- [ ] Deep link `https://<domain>/p/<id>` opens product (set up app links + `assetlinks.json`)
- [ ] Analytics events (if enabled): view_item, search, add_to_wishlist (no PII)

## Tests
- [ ] Component tests: ProductCard, PriceText (discount math), CategoryTile
- [ ] Maestro: browse category → open product → wishlist toggle persists after restart

## Security checks
- Search query length/char limits; rate limit on `/products?q`; no admin fields in public responses (e.g. `is_active` internals)

## Acceptance criteria
- [ ] Home and listing scroll at 60 fps on a 2–3 GB device with 50+ items
- [ ] All states (loading/empty/error/offline) implemented and screenshot-verified
- [ ] Tamil names/descriptions render correctly; search works for English and Tamil
- [ ] Wishlist survives restart and syncs across two devices on the same account
- [ ] No full-resolution images loaded in lists (verify with network inspector)

## Pitfalls
- Unbounded lists without pagination; unoptimised images; ignoring `in_stock` visual state; losing scroll position on back navigation
