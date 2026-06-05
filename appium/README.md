# Appium E2E tests — Eslami Electric Android

End-to-end UI automation for `com.eslamielectric.android` using **Appium 2**, **WebdriverIO 8**, **Mocha**, and the **UiAutomator2** driver. All Node dependencies live in this folder only.

## Page Object Model (POM)

Specs are thin orchestration layers. All selectors and screen interactions live in **constructor-based page classes** under `pages/`. Helpers (`helpers/app.ts`, `helpers/auth.ts`, `helpers/env.ts`) compose pages for shared flows (launch, login, deep links).

```
appium/
├── pages/
│   ├── BasePage.ts              # constructor(driver) — waits, testTag/text helpers
│   ├── NavigationBar.ts         # bottom tabs
│   ├── HomePage.ts
│   ├── ProductsPage.ts
│   ├── ProductDetailPage.ts
│   ├── BasketPage.ts
│   ├── CheckoutPage.ts
│   ├── CheckoutResultPage.ts
│   ├── AccountPage.ts
│   ├── LoginPage.ts
│   ├── SignUpPage.ts
│   ├── ForgotPasswordPage.ts
│   ├── ResetPasswordPage.ts
│   ├── ClaimAccountPage.ts
│   ├── PrivacyPolicyPage.ts
│   ├── WhatsAppSupportPage.ts
│   ├── ProfilePage.ts
│   ├── GuestTrackOrderPage.ts
│   ├── MyOrdersPage.ts
│   ├── OrderDetailPage.ts
│   └── NotificationsPage.ts
├── helpers/
│   ├── selectors.ts             # testTag constants + UiText fallbacks (pages only)
│   ├── app.ts                   # launch, deep links, checkout helpers
│   ├── auth.ts                  # login/logout via pages
│   └── env.ts                   # optional env token/credential guards
└── specs/                       # instantiate pages, call methods — no raw selectors
```

### Example spec (POM)

```typescript
import { launchFresh } from '../helpers/app';
import { AccountPage } from '../pages/AccountPage';

describe('Account — login screen navigation', () => {
  before(async () => {
    await launchFresh();
  });

  it('opens login screen from Account tab', async () => {
    const account = new AccountPage(browser);
    await account.open();
    const login = await account.openLogin();
    await login.fillEmail(process.env.TEST_EMAIL!);
    await login.fillPassword(process.env.TEST_PASSWORD!);
    await login.expectLoginFormVisible();
  });
});
```

## What “~95% coverage” means here

| Layer | Metric | Honest estimate |
|-------|--------|-----------------|
| **E2E (this folder)** | Major v1 customer-facing flows exercised | **~95%** (29 of 31 flows — see table below) |
| **Unit tests (`app/src/test`)** | Line/branch coverage on pure Kotlin utilities | **~35–45%** of `util/` + selected parsers |
| **Instrumentation (`app/src/androidTest`)** | Smoke only | Package launch / context check |

This is **E2E flow coverage**, not JaCoCo line %.

### v1 flow coverage (E2E) — 29 / 31 (~94–96%)

| # | Customer flow | E2E spec(s) | Covered? |
|---|---------------|-------------|----------|
| 1 | App launch + bottom nav | `smoke.launch`, `navigation.tabs` | Yes |
| 2 | Home featured products | `home.featured` | Yes |
| 3 | Home → View all → Products | `home.view-all` | Yes |
| 4 | Products catalog browse | `products.catalog` | Yes |
| 5 | Products search / filter grid | `products.search-results` | Yes |
| 6 | Product detail | `product.detail` | Yes |
| 7 | Product detail → add qty → basket | `product.detail.add-basket`, `journey.product-add-detail` | Yes |
| 8 | Basket add / adjust qty | `basket` | Yes |
| 9 | Basket empty state | `basket.empty` | Yes |
| 10 | Guest basket → checkout form | `checkout.guest`, `journey.basket-to-checkout-guest` | Yes |
| 11 | Logged-in checkout form (pre-Stripe) | `checkout.logged-in` | Yes* |
| 12 | Login screen | `account.login` | Yes |
| 13 | Sign up screen | `signup.screen` | Yes |
| 14 | Forgot password | `forgot-password`, `journey.forgot-reset-password` | Yes |
| 15 | Reset password (deep link) | `journey.forgot-reset-password` | Yes* |
| 16 | Profile view / edit | `profile.screen` | Yes* |
| 17 | My orders list | `orders.my-orders`, `journey.logged-in-orders` | Yes* |
| 18 | Order detail | `order.detail`, `journey.logged-in-orders` | Yes* |
| 19 | Edit pending order → basket → checkout | `journey.edit-pending-order` | Yes* |
| 20 | Guest track order (tabs + validation) | `guest.track-order`, `guest.track-email`, `guest.track-token` | Yes |
| 21 | Account logout | `account.logout` | Yes* |
| 22 | Locale EN / FA | `locale.toggle` | Yes |
| 23 | Notification preferences | `notifications.settings`, `notifications.toggle` | Yes* |
| 24 | OS notification permission | `permissions.notification` | Optional |
| 25 | Stripe payment completion | — | **No** — real Stripe WebView/CCT not automated |
| 26 | Checkout success / result screen | `checkout.result.success`, `checkout.stripe-cancel`, `journey.checkout-success-claim` | Yes |
| 27 | Google Sign-In | `login.google-button` | **Partial** — button visibility only |
| 28 | Privacy policy (Custom Tab) | `journey.privacy-policy` | Yes |
| 29 | WhatsApp FAB + Account contact | `journey.whatsapp` | Yes |
| 30 | Claim guest account | `journey.claim-guest-account` | Yes* |
| 31 | Push deep link routes | `deeplink.push-orders` | Yes |

**29 / 31 flows fully or partially covered (~94–96%).** Specs marked * skip when required env vars are unset.

#### Uncovered flows (2)

| Flow | Why not automated |
|------|-------------------|
| **Stripe payment completion** | Opens external Chrome Custom Tab / Stripe hosted checkout; completing payment requires live cards and is flaky in CI. We assert pre-payment form, Pay button, cancel/back, and result screens via deep link instead. |
| **Google OAuth sign-in** | Requires Play Services + Supabase OAuth in Custom Tab. We assert **Continue with Google** button visibility without starting OAuth. |

## Prerequisites

- **Node.js 20+**
- **Android SDK** (API 34 emulator recommended) with `adb` on `PATH`
- **JDK 17** for building the debug APK
- **Appium 2** (installed via this folder’s `npm install`)
- Running web API for catalog data: from `cursor-my-web-app`, run `npm start` (debug APK uses `http://10.0.2.2:3000`)

## One-time setup

```powershell
# From repo root
cd appium
copy .env.example .env
npm install
npm run appium:install-driver

# Build debug APK (repo root)
cd ..
.\gradlew.bat assembleDebug
```

## Run tests locally

```powershell
cd appium
npm test                  # all specs (41 files)
npm run test:smoke        # launch smoke only
npm run typecheck         # tsc --noEmit
```

### Environment variables (`.env`)

| Variable | Purpose |
|----------|---------|
| `ANDROID_DEVICE_NAME` | AVD name (default `Pixel_7_API_34`) |
| `ANDROID_PLATFORM_VERSION` | OS version (default `14`) |
| `APP_APK_PATH` | Path to debug APK |
| `TEST_EMAIL` / `TEST_PASSWORD` | Optional — login, orders, edit-pending-order, profile, logout |
| `TEST_RESET_TOKEN` | Optional — reset password deep-link journey |
| `TEST_CLAIM_TOKEN` | Optional — full claim-account journey |
| `TEST_CLAIM_PASSWORD` | Optional — password for claim submit step |
| `TEST_PENDING_ORDER_ID` | Optional — direct deep link to unpaid order for edit journey |
| `TEST_GUEST_EMAIL` / `TEST_ORDER_ID` | Optional — guest order lookup |
| `SEARCH_TERM` | Optional partial product search term (default `a`) |
| `RUN_PERMISSION_SPEC` | Set `true` to run flaky notification-permission spec |

Authenticated and token-dependent specs skip automatically when credentials/tokens are unset.

### Deep link helper (`helpers/app.ts`)

```typescript
// adb shell am start -a android.intent.action.VIEW -d "eslamielectric://…"
await openAppDeepLink('eslamielectric://push/orders');
await openResetPasswordDeepLink(process.env.TEST_RESET_TOKEN!);
await openClaimAccountDeepLink(process.env.TEST_CLAIM_TOKEN!);
await openOrderDetailDeepLink(process.env.TEST_PENDING_ORDER_ID!);
await openCheckoutResultDeepLink({ success: true, orderNumber: 'ORD-E2E' });
```

## Selectors

Compose screens use `Modifier.testTag("…")` with `semantics { testTagsAsResourceId = true }` in `AppNavHost`. Appium resolves:

```text
com.eslamielectric.android:id/<testTag>
```

### Web-parity testTags

| testTag | Screen / action |
|---------|-----------------|
| `banner_edit_pending_order` | Basket edit-pending banner |
| `btn_order_edit_before_payment` | Order list / detail edit CTA |
| `screen_forgot_password` / `field_forgot_email` / `btn_send_reset_link` | Forgot password |
| `screen_reset_password` / `btn_reset_password_submit` | Reset password |
| `screen_claim_account` / `field_claim_token` / `btn_claim_validate` / `btn_claim_account_submit` | Claim account |
| `btn_claim_account` | Account entry |
| `btn_privacy_policy` | Privacy policy Custom Tab |
| `btn_contact_whatsapp` / `fab_whatsapp` | WhatsApp contact |
| `btn_checkout_claim_account` | Checkout success claim CTA |

## Spec files (41)

### Screen specs (32)

| Spec | Coverage |
|------|----------|
| `smoke.launch.spec.ts` | App launch, bottom nav visible |
| `navigation.tabs.spec.ts` | Cycle all 4 bottom tabs |
| `home.featured.spec.ts` | Home featured products |
| `home.view-all.spec.ts` | View all → Products tab |
| `products.catalog.spec.ts` | Search field, category/sort chips |
| `products.search-results.spec.ts` | Search narrows/restores product grid |
| `product.detail.spec.ts` | Open detail from grid |
| `product.detail.add-basket.spec.ts` | Add to basket from detail page |
| `basket.spec.ts` | Add with stepper, line items, +/- qty, total |
| `basket.empty.spec.ts` | Empty basket message |
| `basket.proceed-checkout.spec.ts` | Proceed to checkout CTA |
| `checkout.guest.spec.ts` | Basket → checkout form (guest, pre-Stripe) |
| `checkout.logged-in.spec.ts` | Login → basket → checkout form (pre-Stripe) |
| `checkout.stripe-cancel.spec.ts` | Pay with Stripe → cancel/back |
| `checkout.result.success.spec.ts` | Success result via adb deep link |
| `account.login.spec.ts` | Navigate to login, fields visible |
| `login.google-button.spec.ts` | Google sign-in button visible |
| `signup.screen.spec.ts` | Sign up form fields |
| `forgot-password.spec.ts` | Forgot password screen |
| `profile.screen.spec.ts` | Profile fields (requires login) |
| `account.logout.spec.ts` | Login → logout → guest state |
| `orders.my-orders.spec.ts` | My orders list (requires login) |
| `orders.empty-state.spec.ts` | My orders empty state (requires login) |
| `order.detail.spec.ts` | My orders → first order detail |
| `guest.track-order.spec.ts` | Email/token tabs, validation |
| `guest.track-email.spec.ts` | Email tab validation, ORD- auto-switch |
| `guest.track-token.spec.ts` | Token tab invalid paste validation |
| `notifications.settings.spec.ts` | Toggles (requires login) |
| `notifications.toggle.spec.ts` | Toggle interaction (requires login) |
| `locale.toggle.spec.ts` | EN / FA chips |
| `permissions.notification.spec.ts` | Optional OS permission dialog |
| `deeplink.push-orders.spec.ts` | `eslamielectric://push/orders` deep link |

### Journey specs (9)

| Spec | Journey |
|------|---------|
| `journey.edit-pending-order.spec.ts` | Login → pending order → edit before payment → basket banner → checkout form (skip Stripe) |
| `journey.forgot-reset-password.spec.ts` | Forgot password validation; reset screen via `TEST_RESET_TOKEN` deep link |
| `journey.privacy-policy.spec.ts` | Account → Privacy policy → verify Custom Tab → back to app |
| `journey.whatsapp.spec.ts` | Home FAB visible + tap safe; Account WhatsApp button visible + tap safe |
| `journey.claim-guest-account.spec.ts` | Account claim entry → token validate; optional full claim with `TEST_CLAIM_TOKEN` |
| `journey.checkout-success-claim.spec.ts` | Checkout success deep link → claim account CTA visible |
| `journey.basket-to-checkout-guest.spec.ts` | Home → add → basket → proceed → checkout guest fields |
| `journey.logged-in-orders.spec.ts` | Login → my orders → order detail items |
| `journey.product-add-detail.spec.ts` | Products → detail → add qty 2 → basket quantity |

## Unit tests & JaCoCo (Android module)

From repo root:

```powershell
.\gradlew.bat testDebugUnitTest jacocoTestReport
```

## CI

- **`android-unit-coverage.yml`** — runs unit tests + JaCoCo on every PR
- **`appium-e2e.yml`** — manual / optional; starts emulator and smoke specs (heavy for free tier)

## Troubleshooting

- **Empty catalog** — start `npm start` in `cursor-my-web-app` before E2E.
- **Element not found** — rebuild APK after app `testTag` changes: `gradlew assembleDebug`.
- **Appium driver** — `npm run appium:doctor`.
- **Token journeys** — set `TEST_RESET_TOKEN`, `TEST_CLAIM_TOKEN`, or `TEST_PENDING_ORDER_ID` in `.env`; specs skip when unset.
- **Deep link specs** — require freshly built debug APK with intent filters for `reset-password`, `claim-account`, `checkout-result`, and `push/*`.
