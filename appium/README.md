# Appium E2E tests — Eslami Electric Android

End-to-end UI automation for `com.eslamielectric.android` using **Appium 2**, **WebdriverIO 8**, **Mocha**, and the **UiAutomator2** driver. All Node dependencies live in this folder only.

## What “~50% coverage” means here

| Layer | Metric | Honest estimate |
|-------|--------|-----------------|
| **E2E (this folder)** | Major v1 customer-facing flows / screens exercised | **~50%** (12 of 24 flows — see table below) |
| **Unit tests (`app/src/test`)** | Line/branch coverage on pure Kotlin utilities | **~35–45%** of `util/` + selected parsers — not full ViewModel/repository coverage |
| **Instrumentation (`app/src/androidTest`)** | Smoke only | Package launch / context check |

This is **E2E flow coverage**, not JaCoCo line %. JaCoCo reports **debug unit-test line coverage** across compiled app classes; expect **~15–25% overall app code** until ViewModels and repositories get mocked network tests.

### v1 flow coverage (E2E)

| # | Customer flow | E2E spec(s) | Covered? |
|---|---------------|-------------|----------|
| 1 | App launch + bottom nav | `smoke.launch`, `navigation.tabs` | Yes |
| 2 | Home featured products | `home.featured` | Yes |
| 3 | Home → View all → Products | `home.view-all` | Yes |
| 4 | Products catalog browse | `products.catalog` | Yes |
| 5 | Products search / filter grid | `products.search-results` | Yes |
| 6 | Product detail | `product.detail` | Yes |
| 7 | Basket add / adjust qty | `basket` | Yes |
| 8 | Basket empty state | `basket.empty` | Yes |
| 9 | Guest checkout form (pre-Stripe) | `checkout.guest` | Yes |
| 10 | Logged-in checkout form (pre-Stripe) | `checkout.logged-in` | Yes* |
| 11 | Login screen | `account.login` | Yes |
| 12 | Sign up screen | `signup.screen` | Yes |
| 13 | Forgot password | `forgot-password` | Yes |
| 14 | Profile view / edit | `profile.screen` | Yes* |
| 15 | My orders list | `orders.my-orders` | Yes* |
| 16 | Order detail | `order.detail` | Yes* |
| 17 | Guest track order (tabs + validation) | `guest.track-order`, `guest.track-email` | Yes |
| 18 | Account logout | `account.logout` | Yes* |
| 19 | Locale EN / FA | `locale.toggle` | Yes |
| 20 | Notification preferences | `notifications.settings` | Yes* |
| 21 | OS notification permission | `permissions.notification` | Optional |
| 22 | Stripe payment completion | — | **No** (external WebView; not automated) |
| 23 | Checkout success / result screen | — | **No** (requires Stripe) |
| 24 | Google Sign-In | — | **No** (OAuth / Play Services) |

**12 / 24 flows covered (~50%).** Specs marked * skip when `TEST_EMAIL` / `TEST_PASSWORD` are unset.

Screens without dedicated `testTag`s (checkout, signup, profile, etc.) use stable English `textContains` selectors via `helpers/selectors.ts` → `UiText`. No new app `testTag`s were required for this milestone.

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

Create an AVD (example):

```text
Pixel_7_API_34 — Android 14 (API 34)
```

Start the emulator, then verify:

```powershell
adb devices
```

## Run tests locally

Terminal 1 — emulator running, web API on port 3000.

Terminal 2 — Appium (optional if using WDIO’s built-in service):

```powershell
cd appium
npx appium
```

Terminal 3 — tests:

```powershell
cd appium
npm test                  # all specs (23 files)
npm run test:smoke        # launch smoke only
```

### Environment variables (`.env`)

| Variable | Purpose |
|----------|---------|
| `ANDROID_DEVICE_NAME` | AVD name (default `Pixel_7_API_34`) |
| `ANDROID_PLATFORM_VERSION` | OS version (default `14`) |
| `APP_APK_PATH` | Path to debug APK |
| `TEST_EMAIL` / `TEST_PASSWORD` | Optional — enables login / checkout / profile / orders / logout specs |
| `TEST_GUEST_EMAIL` / `TEST_ORDER_ID` | Optional — reserved for future guest-order lookup with real data |
| `SEARCH_TERM` | Optional partial product search term (default `a`) |
| `RUN_PERMISSION_SPEC` | Set `true` to run flaky notification-permission spec |

Authenticated specs skip automatically when credentials are unset.

## Selectors

Compose screens use `Modifier.testTag("…")` with `semantics { testTagsAsResourceId = true }` in `AppNavHost`. Appium resolves:

```text
com.eslamielectric.android:id/<testTag>
```

See `helpers/selectors.ts` for testTags and `UiText` for text-based fallbacks.

## Spec files (23)

| Spec | Coverage |
|------|----------|
| `smoke.launch.spec.ts` | App launch, bottom nav visible |
| `navigation.tabs.spec.ts` | Cycle all 4 bottom tabs |
| `home.featured.spec.ts` | Home featured products |
| `home.view-all.spec.ts` | View all → Products tab |
| `products.catalog.spec.ts` | Search field, category/sort chips |
| `products.search-results.spec.ts` | Search narrows/restores product grid |
| `product.detail.spec.ts` | Open detail from grid |
| `basket.spec.ts` | Add with stepper, line items, +/- qty, total |
| `basket.empty.spec.ts` | Empty basket message |
| `checkout.guest.spec.ts` | Basket → checkout form (guest, pre-Stripe) |
| `checkout.logged-in.spec.ts` | Login → basket → checkout form (pre-Stripe) |
| `account.login.spec.ts` | Navigate to login, fields visible |
| `signup.screen.spec.ts` | Sign up form fields |
| `forgot-password.spec.ts` | Forgot password screen |
| `profile.screen.spec.ts` | Profile fields (requires login) |
| `account.logout.spec.ts` | Login → logout → guest state |
| `orders.my-orders.spec.ts` | My orders list (requires login) |
| `order.detail.spec.ts` | My orders → first order detail |
| `guest.track-order.spec.ts` | Email/token tabs, validation |
| `guest.track-email.spec.ts` | Email tab validation, ORD- auto-switch |
| `notifications.settings.spec.ts` | Toggles (requires login) |
| `locale.toggle.spec.ts` | EN / FA chips |
| `permissions.notification.spec.ts` | Optional OS permission dialog |

## Unit tests & JaCoCo (Android module)

From repo root:

```powershell
.\gradlew.bat testDebugUnitTest jacocoTestReport
```

HTML report:

```text
app/build/reports/jacoco/jacocoTestReport/html/index.html
```

## CI

- **`android-unit-coverage.yml`** — runs unit tests + JaCoCo on every PR
- **`appium-e2e.yml`** — manual / optional; starts emulator and smoke specs (heavy for free tier)

## Troubleshooting

- **Empty catalog** — start `npm start` in `cursor-my-web-app` before E2E.
- **Element not found** — rebuild APK after app `testTag` changes: `gradlew assembleDebug`.
- **Appium driver** — `npm run appium:doctor`.
- **Checkout specs** — assert form fields only; Stripe WebView payment is intentionally out of scope.
