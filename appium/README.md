# Appium E2E tests — Eslami Electric Android

End-to-end UI automation for `com.eslamielectric.android` using **Appium 2**, **WebdriverIO 8**, **Mocha**, and the **UiAutomator2** driver. All Node dependencies live in this folder only.

## What “~80% coverage” means here

| Layer | Metric | Honest estimate |
|-------|--------|-----------------|
| **E2E (this folder)** | Major user-facing flows / screens exercised | **~80%** of v1 customer flows (home, catalog, basket, account/login, guest track, locale, product detail, notifications when logged in) |
| **Unit tests (`app/src/test`)** | Line/branch coverage on pure Kotlin utilities | **~35–45%** of `util/` + selected parsers — not full ViewModel/repository coverage |
| **Instrumentation (`app/src/androidTest`)** | Smoke only | Package launch / context check |

JaCoCo reports **debug unit-test line coverage** across compiled app classes; expect **~15–25% overall app code** until ViewModels and repositories get mocked network tests. The goal is to grow testable layers over time, not claim 80% JaCoCo on the whole app yet.

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
npm test                  # all specs
npm run test:smoke        # launch smoke only
```

### Environment variables (`.env`)

| Variable | Purpose |
|----------|---------|
| `ANDROID_DEVICE_NAME` | AVD name (default `Pixel_7_API_34`) |
| `ANDROID_PLATFORM_VERSION` | OS version (default `14`) |
| `APP_APK_PATH` | Path to debug APK |
| `TEST_EMAIL` / `TEST_PASSWORD` | Optional — enables login / my-orders / notifications specs |
| `RUN_PERMISSION_SPEC` | Set `true` to run flaky notification-permission spec |

Specs **`orders.my-orders.spec.ts`** and **`notifications.settings.spec.ts`** skip automatically when credentials are unset.

## Selectors

Compose screens use `Modifier.testTag("…")` with `semantics { testTagsAsResourceId = true }` in `AppNavHost`. Appium resolves:

```text
com.eslamielectric.android:id/<testTag>
```

See `helpers/selectors.ts` for the full list.

## Spec files

| Spec | Coverage |
|------|----------|
| `smoke.launch.spec.ts` | App launch, bottom nav visible |
| `home.featured.spec.ts` | Home featured products, view all |
| `products.catalog.spec.ts` | Search, category chip, sort chip |
| `basket.spec.ts` | Add with stepper, line items, +/- qty, total |
| `account.login.spec.ts` | Navigate to login, fields visible |
| `guest.track-order.spec.ts` | Email/token tabs, validation |
| `notifications.settings.spec.ts` | Toggles (requires login) |
| `orders.my-orders.spec.ts` | My orders list (requires login) |
| `product.detail.spec.ts` | Open detail from grid |
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
