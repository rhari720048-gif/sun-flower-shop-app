# 02 – Database Schema (Neon PostgreSQL)

Conventions: `uuid` primary keys (`gen_random_uuid()`), `timestamptz` for all times, money as integer paise,
`snake_case`, every table has `created_at`, mutable tables have `updated_at` (trigger). Migrations via Drizzle Kit,
never edit the DB by hand in staging/prod. Runtime DB role has least privilege (no DDL).

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TYPE user_role AS ENUM ('customer','admin');
CREATE TYPE order_status AS ENUM ('placed','preparing','out_for_delivery','delivered','cancelled');
CREATE TYPE payment_method AS ENUM ('cod','online');
CREATE TYPE payment_status AS ENUM ('pending','paid','failed','refund_pending','refunded');

CREATE TABLE users (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  firebase_uid  text NOT NULL UNIQUE,
  name          text,
  phone         text,                       -- E.164, e.g. +919876543210
  email         text,
  role          user_role NOT NULL DEFAULT 'customer',
  language      text NOT NULL DEFAULT 'en' CHECK (language IN ('en','ta')),
  is_deleted    boolean NOT NULL DEFAULT false,
  deleted_at    timestamptz,
  created_at    timestamptz NOT NULL DEFAULT now(),
  updated_at    timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX users_phone_idx ON users(phone);

CREATE TABLE device_tokens (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  fcm_token   text NOT NULL UNIQUE,
  platform    text NOT NULL DEFAULT 'android',
  last_seen_at timestamptz NOT NULL DEFAULT now(),
  created_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX device_tokens_user_idx ON device_tokens(user_id);

CREATE TABLE addresses (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  label       text NOT NULL CHECK (label IN ('home','work','other')),
  recipient_name text NOT NULL,
  recipient_phone text NOT NULL,
  line1       text NOT NULL,
  line2       text,
  landmark    text,
  pincode     text NOT NULL CHECK (pincode ~ '^[0-9]{6}$'),
  lat         double precision NOT NULL CHECK (lat BETWEEN -90 AND 90),
  lng         double precision NOT NULL CHECK (lng BETWEEN -180 AND 180),
  distance_km numeric(5,2) NOT NULL,        -- computed server-side on save
  is_default  boolean NOT NULL DEFAULT false,
  created_at  timestamptz NOT NULL DEFAULT now(),
  updated_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX addresses_user_idx ON addresses(user_id);
CREATE UNIQUE INDEX one_default_address ON addresses(user_id) WHERE is_default;

CREATE TABLE categories (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name_en     text NOT NULL,
  name_ta     text NOT NULL,
  image_url   text,
  sort_order  int NOT NULL DEFAULT 0,
  is_active   boolean NOT NULL DEFAULT true,
  created_at  timestamptz NOT NULL DEFAULT now(),
  updated_at  timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE products (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  category_id  uuid NOT NULL REFERENCES categories(id),
  name_en      text NOT NULL,
  name_ta      text NOT NULL,
  description_en text,
  description_ta text,
  price_paise  int NOT NULL CHECK (price_paise >= 0),
  mrp_paise    int CHECK (mrp_paise IS NULL OR mrp_paise >= price_paise),
  images       text[] NOT NULL DEFAULT '{}',
  unit_label   text,                        -- e.g. "12 roses", "1 string"
  in_stock     boolean NOT NULL DEFAULT true,
  is_featured  boolean NOT NULL DEFAULT false,
  is_active    boolean NOT NULL DEFAULT true,
  created_at   timestamptz NOT NULL DEFAULT now(),
  updated_at   timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX products_category_idx ON products(category_id) WHERE is_active;
CREATE INDEX products_featured_idx ON products(is_featured) WHERE is_active AND is_featured;
CREATE INDEX products_search_idx ON products USING gin (to_tsvector('simple', name_en || ' ' || name_ta));

CREATE TABLE wishlist_items (
  user_id     uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  product_id  uuid NOT NULL REFERENCES products(id) ON DELETE CASCADE,
  created_at  timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, product_id)
);

CREATE TABLE banners (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  image_url   text NOT NULL,
  title       text,
  target_type text CHECK (target_type IN ('category','product','none')) DEFAULT 'none',
  target_id   uuid,
  sort_order  int NOT NULL DEFAULT 0,
  is_active   boolean NOT NULL DEFAULT true,
  created_at  timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE delivery_partners (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name        text NOT NULL,
  phone       text NOT NULL,
  photo_url   text,
  is_active   boolean NOT NULL DEFAULT true,
  created_at  timestamptz NOT NULL DEFAULT now()
);

-- Single-row settings table
CREATE TABLE shop_settings (
  id                  smallint PRIMARY KEY DEFAULT 1 CHECK (id = 1),
  is_open             boolean NOT NULL DEFAULT true,
  closed_message      text,                  -- admin customizable, Unicode (Tamil OK), max 300 chars
  reopen_at           timestamptz,
  shop_name           text NOT NULL DEFAULT 'Sun Flowers and Decoration',
  shop_lat            double precision NOT NULL,
  shop_lng            double precision NOT NULL,
  delivery_radius_km  numeric(4,1) NOT NULL DEFAULT 6.0 CHECK (delivery_radius_km > 0 AND delivery_radius_km <= 25),
  delivery_charge_paise int NOT NULL DEFAULT 4000,
  free_delivery_above_paise int,
  min_order_paise     int NOT NULL DEFAULT 0,
  tax_percent         numeric(4,2) NOT NULL DEFAULT 0,
  estimated_delivery_minutes int NOT NULL DEFAULT 30,
  support_phone       text,
  support_whatsapp    text,
  min_supported_version text NOT NULL DEFAULT '1.0.0',  -- forced-update gate (semver)
  cod_enabled         boolean NOT NULL DEFAULT true,
  online_payment_enabled boolean NOT NULL DEFAULT false,
  updated_by          uuid REFERENCES users(id),
  updated_at          timestamptz NOT NULL DEFAULT now(),
  CHECK (char_length(coalesce(closed_message,'')) <= 300)
);

CREATE TABLE orders (
  id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  order_number     bigint GENERATED ALWAYS AS IDENTITY UNIQUE,  -- shown as SF-000123
  user_id          uuid NOT NULL REFERENCES users(id),
  idempotency_key  text NOT NULL,
  status           order_status NOT NULL DEFAULT 'placed',
  address_snapshot jsonb NOT NULL,          -- full copy of address at order time
  distance_km      numeric(5,2) NOT NULL,
  subtotal_paise   int NOT NULL,
  delivery_charge_paise int NOT NULL,
  tax_paise        int NOT NULL,
  total_paise      int NOT NULL CHECK (total_paise >= 0),
  payment_method   payment_method NOT NULL,
  payment_status   payment_status NOT NULL DEFAULT 'pending',
  cashfree_order_id text UNIQUE,            -- cf_order_id returned by Cashfree
  cashfree_payment_session_id text,         -- short-lived; never log
  cashfree_payment_id text UNIQUE,          -- cf_payment_id of the successful attempt
  payment_expires_at timestamptz,           -- unpaid online orders auto-cancel after this
  delivery_partner_id uuid REFERENCES delivery_partners(id),
  customer_note    text CHECK (char_length(customer_note) <= 200),
  cancel_reason    text,
  cancelled_by     text CHECK (cancelled_by IN ('customer','admin')),
  placed_at        timestamptz NOT NULL DEFAULT now(),
  out_for_delivery_at timestamptz,
  delivered_at     timestamptz,
  updated_at       timestamptz NOT NULL DEFAULT now(),
  UNIQUE (user_id, idempotency_key)
);
CREATE INDEX orders_user_idx ON orders(user_id, placed_at DESC);
CREATE INDEX orders_status_idx ON orders(status, placed_at DESC);

CREATE TABLE order_items (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  order_id     uuid NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  product_id   uuid REFERENCES products(id) ON DELETE SET NULL,
  name_snapshot text NOT NULL,
  image_snapshot text,
  unit_price_paise int NOT NULL,
  quantity     int NOT NULL CHECK (quantity BETWEEN 1 AND 50),
  line_total_paise int NOT NULL
);
CREATE INDEX order_items_order_idx ON order_items(order_id);

CREATE TABLE order_status_history (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  order_id    uuid NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  status      order_status NOT NULL,
  changed_by  uuid REFERENCES users(id),
  note        text,
  created_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX osh_order_idx ON order_status_history(order_id, created_at);

CREATE TABLE payment_events (               -- raw Cashfree webhook log for audit/idempotency
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  event_id    text NOT NULL UNIQUE,
  event_type  text NOT NULL,
  payload     jsonb NOT NULL,
  processed   boolean NOT NULL DEFAULT false,
  created_at  timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE admin_audit_log (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  admin_id    uuid NOT NULL REFERENCES users(id),
  action      text NOT NULL,                -- e.g. 'shop.toggle','product.update'
  entity      text,
  entity_id   text,
  diff        jsonb,
  created_at  timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE legal_documents (              -- optional: versioned policies served to app
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  type        text NOT NULL CHECK (type IN ('privacy','terms','refund','shipping')),
  version     text NOT NULL,
  url         text NOT NULL,
  effective_at timestamptz NOT NULL
);
```

## Triggers
`updated_at` trigger on every table having the column:
```sql
CREATE OR REPLACE FUNCTION set_updated_at() RETURNS trigger AS $$
BEGIN NEW.updated_at = now(); RETURN NEW; END; $$ LANGUAGE plpgsql;
-- CREATE TRIGGER ... BEFORE UPDATE ON <table> FOR EACH ROW EXECUTE FUNCTION set_updated_at();
```

## Seed data (seed script, idempotent)
- `shop_settings` row with real shop coordinates (from owner)
- Categories: Bouquets, Arrangements, Garlands, Flower Baskets, Decorations (Tamil names filled)
- First admin: script `npm run seed:admin -- --phone +91XXXXXXXXXX` marks an existing user (after they log in once) as admin. Never expose an endpoint to promote admins.

## Data retention and deletion
- Account deletion: set `is_deleted`, null/replace `name, phone, email`, delete `addresses`, `device_tokens`, `wishlist_items`; keep `orders` with `address_snapshot` redacted (keep only area/pincode) for accounting. Also delete the Firebase user.
- Purge `device_tokens` not seen for 60 days.
- Backups: enable point-in-time recovery; test a restore once before launch.

## Query rules
- Never `SELECT *` in API responses; use explicit column lists and response mappers.
- Wrap order creation in one transaction (`SELECT ... FOR UPDATE` on product rows not required for a flower shop, but re-read prices and stock inside the transaction).
