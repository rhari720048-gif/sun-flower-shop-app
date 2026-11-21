# Sun Flowers and Decoration – Mobile App Documentation

Flower delivery app (Android first, Play Store release) for **Sun Flowers and Decoration**, Tamil Nadu.
Customers order flowers; the shop delivers within a **5–6 km radius**. The shop owner controls
open/closed status and a custom message from an in-app Admin section.

## Locked tech stack

| Layer | Technology |
|---|---|
| Mobile app | React Native + Expo (dev builds + EAS), TypeScript (strict) |
| Authentication | Firebase Authentication (Phone OTP + Google Sign-In) |
| Database | Neon (serverless PostgreSQL) |
| Backend API | Node.js 20+, TypeScript, Fastify, Drizzle ORM, Zod |
| Push notifications | Firebase Cloud Messaging (FCM) |
| Image storage | Firebase Storage (or Cloudinary) |
| Payments | Cashfree Payments (UPI/cards/netbanking) + Cash on Delivery |
| Maps | Google Maps SDK (react-native-maps) + Places/Geocoding |
| Monitoring | Firebase Crashlytics (app), Sentry (backend), uptime monitor |
| CI/CD | GitHub Actions + EAS Build/Submit |

> The app NEVER connects directly to Neon. All data goes through the backend API.
> Firebase is used only for Auth, FCM, Storage, Crashlytics, App Check.

## How to use these docs

Read the reference docs first, then execute phases **in order**. Never start a phase until the
previous phase's Definition of Done is fully met.

### Reference documents (read first)
| File | Purpose |
|---|---|
| `00-project-overview.md` | Scope, roles, user stories, business rules, decisions |
| `01-architecture.md` | System architecture, flows, environments, hosting |
| `02-database-schema.md` | Complete PostgreSQL schema, constraints, indexes, seeds |
| `03-api-spec.md` | Every endpoint, request/response, errors, rules |
| `04-design-system.md` | Colours, typography, components, every screen spec, UX states |
| `05-engineering-standards.md` | Code quality rules (senior-developer standard) |
| `06-security.md` | Security requirements (OWASP MASVS + API Top 10) |
| `07-testing-strategy.md` | Test pyramid, tools, coverage, QA test cases |
| `08-playstore-checklist.md` | Every Play Console requirement and policy item |
| `09-legal-templates.md` | Privacy Policy, Terms, Refund/Shipping policy templates |
| `10-ai-builder-instructions.md` | Instruction prompt if an AI coding tool builds the code |

### Phase documents (`phases/`)
| Phase | Title | Est. time |
|---|---|---|
| 01 | Foundation, environments, tooling | 3–4 days |
| 02 | Backend core, database, authentication | 5–6 days |
| 03 | Mobile app shell, design system, auth UI | 5–6 days |
| 04 | Catalog, search, wishlist | 5 days |
| 05 | Shop status, addresses, delivery radius | 5 days |
| 06 | Cart, checkout, orders, payments | 7–8 days |
| 07 | Admin module | 7–8 days |
| 08 | Notifications, tracking, account, support | 5 days |
| 09 | Security hardening, performance, accessibility, i18n | 5 days |
| 10 | Testing and QA | 6–7 days |
| 11 | Play Store compliance and release | 4 days + 14-day closed test |
| 12 | Post-launch operations and roadmap | ongoing |

Total: roughly 10–12 weeks for one developer working steadily.

## Global Definition of Done (applies to EVERY phase)

- [ ] TypeScript strict, zero `any` without justification, zero lint errors, zero type errors
- [ ] No hard-coded secrets, URLs, fake data, `console.log`, TODO or placeholder text left in code
- [ ] Every screen has loading, empty, error and offline states
- [ ] Every API input validated with Zod; every endpoint has authorization checks
- [ ] Unit/integration tests for new logic pass in CI
- [ ] Works on a low-end Android device (2–3 GB RAM) and on Android 8+ up to latest
- [ ] Tamil and English strings used (no hard-coded UI text)
- [ ] Docs/changelog updated; code reviewed; merged via pull request
