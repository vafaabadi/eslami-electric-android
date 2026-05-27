# Eslami Electric — Android (customer v1)

Native **Kotlin + Jetpack Compose** customer app for [Eslami Electric](https://www.eslamielectric.com). Not a TWA.

| Topic | Location |
|--------|----------|
| HTTP API contract | [mobile-api.md](https://github.com/vafaabadi/EslamiElectric/blob/main/docs/mobile-api.md) (local: [`cursor-my-web-app/docs/mobile-api.md`](../cursor-my-web-app/docs/mobile-api.md)) |
| Web backend | [EslamiElectric](https://github.com/vafaabadi/EslamiElectric) (local: [`cursor-my-web-app`](../cursor-my-web-app)) |

## Prerequisites

- **Android Studio** Ladybug (2024.2+) or newer with SDK 34
- **JDK 17** (Embedded JDK in Android Studio is fine)
- Running web API for debug (default `http://localhost:3000`) or a deployed staging/production URL

## Repository

```bash
git clone https://github.com/vafaabadi/eslami-electric-android.git
cd eslami-electric-android
```

## Open the project

1. Clone this repo (or copy from `cursor-my-web-app/android-export` if you used the fallback path).
2. Android Studio → **Open** → select this folder.
3. Copy `local.properties.example` → `local.properties` if Studio does not create it (set `sdk.dir` to your Android SDK).
4. **Sync Project with Gradle Files**, then Run on emulator or device.

### Gradle wrapper

If `gradlew.bat` / `gradle-wrapper.jar` are missing, either:

- Let Android Studio generate the wrapper on first sync, or
- From the project root (with Gradle installed): `gradle wrapper --gradle-version 8.7`

Verify from project root (requires **JDK 17** on `JAVA_HOME`):

```bat
gradlew.bat tasks
```

If Gradle reports Java 8, set `JAVA_HOME` to JDK 17 or adjust `org.gradle.java.home` in `gradle.properties`.

### Gradle sync failed / JDK

**Symptoms:** “No matching variant of `com.android.tools.build:gradle:8.5.2`”, Java 11 compatibility warnings, only **Gradle Scripts** visible (no **app** module), Run disabled.

**Cause:** This project uses **Android Gradle Plugin 8.5.2**, which needs **Gradle 8.7+** and **JDK 17**. Android Studio was using an old **Java 8 JRE** (`Android Studio\jre`) instead of **JBR 17**.

**Fix in Android Studio (recommended):**

1. **File → Settings** (Windows) or **Android Studio → Settings** (macOS).
2. **Build, Execution, Deployment → Build Tools → Gradle**.
3. **Gradle JDK** → choose **Embedded JDK (17)** or **jbr-17** (not `jre` / Java 8).
4. If no JDK 17 is listed: **Download JDK…** → version **17**, vendor **JetBrains Runtime** or **Eclipse Temurin**.
5. **Apply → OK**, then **File → Sync Project with Gradle Files**.

**Fix via `gradle.properties` (when Studio keeps picking Java 8):**

Default path (modern Android Studio):

```properties
org.gradle.java.home=C\:\\Program Files\\Android\\Android Studio\\jbr
```

Older Studio installs without a `jbr` folder — use a standalone JDK 17 instead (uncomment/adjust in `gradle.properties`):

```properties
org.gradle.java.home=C\:\\Program Files\\Java\\jdk-17
```

**Verify from project root:**

```bat
set JAVA_HOME=C:\Program Files\Java\jdk-17
gradlew.bat tasks
```

You should see `:app` tasks (e.g. `assembleDebug`) and the **app** module in the Project view after a successful sync.

## API base URL

Configured via `BuildConfig.API_BASE_URL` in `app/build.gradle.kts`:

| Build type | Default |
|------------|---------|
| **debug** | `http://10.0.2.2:3000` (Android emulator → host machine `localhost:3000`) |
| **release** | `https://www.eslamielectric.com` |

Override release/staging in `app/build.gradle.kts` or add product flavors. Physical device debugging: use your machine LAN IP, e.g. `http://192.168.1.10:3000`, and ensure cleartext is allowed (already enabled for debug in `AndroidManifest.xml`).

**Do not commit** API keys or Stripe secrets; the app only calls your backend over HTTPS (or HTTP in local debug).

### Physical device testing

| Goal | API base URL |
|------|----------------|
| Emulator + local web API | Debug build (default `http://10.0.2.2:3000`) |
| Physical phone + local web API | Change debug `API_BASE_URL` in `app/build.gradle.kts` to your PC LAN IP, e.g. `http://192.168.1.10:3000` (cleartext allowed in debug via `src/debug/res/xml/network_security_config.xml`) |
| Physical phone + production | Install a **release** build (`https://www.eslamielectric.com`) — no cleartext |

## Versioning (Play Store)

| Field | Location | Rule |
|-------|----------|------|
| `versionCode` | `app/build.gradle.kts` → `defaultConfig` | **Integer, must increase** on every Play upload (1, 2, 3, …) |
| `versionName` | same | User-visible semver (e.g. `1.0.0`, `1.0.1`) |

Current: **versionCode 1**, **versionName 1.0.0**.

## Release signing

Secrets stay **local only** — never commit `keystore.properties`, `*.jks`, or `*.keystore`.

1. Copy `keystore.properties.example` → `keystore.properties` (gitignored).
2. Create an upload keystore (Play App Signing recommended):

```powershell
.\scripts\create-upload-keystore.ps1
```

Or manually:

```bat
mkdir release
keytool -genkeypair -v -keystore release/upload-keystore.jks -alias upload -keyalg RSA -keysize 2048 -validity 9125 -storetype PKCS12
```

3. Fill `keystore.properties` with `STORE_FILE`, passwords, and `KEY_ALIAS`.
4. Build a signed release bundle:

```bat
gradlew.bat bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

**Without `keystore.properties`:** `bundleRelease` still runs and produces an **unsigned** AAB (useful for CI). Google Play requires a signed upload; use Play App Signing and register your upload key on first upload.

**Minify:** `isMinifyEnabled` is **off** for v1. Retrofit and kotlinx.serialization need validation against `proguard-rules.pro` before turning minify on in a later release.

## Play Console checklist

Use this before promoting beyond internal testing.

| Step | Notes |
|------|--------|
| **Developer account** | [Google Play Console](https://play.google.com/console) — one-time **$25** registration |
| **App signing** | Enable **Play App Signing**; keep upload keystore backup offline |
| **Privacy policy** | **https://www.eslamielectric.com/en/privacy** (also `/fa/privacy`). Paste this URL in Play Console → App content → Privacy policy |
| **Data safety** | Declare: account info (email, profile), order/payment data processed via your backend; payments via **Stripe**; **no data sold** |
| **Content rating** | Complete IARC questionnaire (shopping / payments) |
| **Store listing** | Short + full description — copy in [`play-store/store-listing-en.md`](play-store/store-listing-en.md) and [`play-store/store-listing-fa.md`](play-store/store-listing-fa.md); **EN + FA** screenshots (phone 16:9 or 9:16); feature graphic 1024×500; app icon **`play-store/icon-512.png`** (512×512 PNG) |
| **Target API** | `targetSdk` / `compileSdk` **34** (adjust when Google raises minimums) |
| **Internal testing** | Play Console → **Testing → Internal testing** → Create release → upload `app-release.aab` → add tester emails → share opt-in link |
| **Stripe** | Release build hits production API; ensure backend Stripe **live** keys match production checkout |

### Play Store assets

| Asset | Path | Notes |
|-------|------|--------|
| **App icon (Play Console upload)** | [`play-store/icon-512.png`](play-store/icon-512.png) | **512×512** PNG, 32-bit with alpha. Play Console → **Grow** → **Store presence** → **Main store listing** → **App icon**. |
| **Feature graphic** | [`play-store/feature-graphic-1024x500.png`](play-store/feature-graphic-1024x500.png) | **1024×500** PNG. Regenerate: `python scripts/generate-feature-graphic.py` (requires `pip install Pillow`). |
| **Screenshots (phone + tablet)** | [`play-store/screenshots/`](play-store/screenshots/) | Phone **1080×2400**, 7-inch **1200×1920**, 10-inch **1600×2560** (4 screens each). Regenerate: `python scripts/generate-play-screenshots.py`. Upload mapping in [`play-store/screenshots/README.md`](play-store/screenshots/README.md). Prefer adb captures when an emulator is running. |
| **Canonical source (web)** | [`cursor-my-web-app/public/icons/icon-512.png`](../cursor-my-web-app/public/icons/icon-512.png) | Regenerate with `scripts/generate-pwa-icons.ps1` or export from `public/icons/icon.svg`, then recopy into `play-store/`. |

### After internal testing

1. Closed / open testing tracks (optional).
2. Production release → **Countries**, **Pricing** (free app).
3. Monitor pre-launch report and crash/ANR dashboards.

## v1 scope (this scaffold)

- Bottom navigation shell: Home, Products, Basket, Account
- Retrofit `ApiService` aligned with `mobile-api.md`
- Local basket (DataStore JSON, same fields as web `localStorage` `basket`)
- JWT in EncryptedSharedPreferences; locale in DataStore
- RTL via `CompositionLocalLayoutDirection` when locale is `fa`
- Stripe Checkout helper: `StripeCheckoutTabs` (AndroidX Browser Custom Tabs)
- EN / FA `strings.xml`
- Launcher icon copied from web `public/icons/icon-192.png`

**Phase 2 (catalog):** Products tab loads `GET /api/products` with loading/error/empty states; Home shows first 6 products + “View all products”; shared `ProductCard` (Coil, USD price, add to basket); Basket tab with qty +/-, remove, and total.

**Phase 3 (auth & account):** Login, sign up, forgot password, profile edit; JWT in `SessionStore`; OkHttp attaches `Authorization: Bearer`; 401 on `/api/me` clears token and returns to login.

**Phase 4 (checkout):** Basket → checkout screen; delivery/collection; guest or logged-in checkout; Stripe Custom Tab; return handling via `confirm-by-session` + `by-session`; success/incomplete result screen.

**Phase 5 (orders):** My orders list + detail (logged in); guest track (email + order number or tracking token); pending cancel / resume Stripe checkout; 401 clears session.

**Phase 6 (polish & web parity):** FA/EN locale toggle with RTL; product detail screen; products search + category chips; basket tab badge; pull-to-refresh; guest order deep links; lightweight loading placeholders.

**Phase 3b:** Google OAuth (Supabase + `POST /api/auth/token`); Telegram not in Android v1.

**Not in v1:** Admin APIs.

## Build commands

```bat
gradlew.bat assembleDebug
gradlew.bat bundleRelease
```

Requires **JDK 17** on `JAVA_HOME`. Signed `bundleRelease` needs `keystore.properties` (see [Release signing](#release-signing)).

## Package layout

```
app/src/main/java/com/eslamielectric/android/
  core/network/     ApiService, DTOs, Retrofit
  core/data/        BasketRepository, SessionStore
  feature/catalog/  CatalogRepository, CatalogViewModel
  ui/components/    ProductCard, CatalogContent, BasketLineRow
  ui/screens/       Home, Products, Basket, Checkout, auth/account screens
  feature/basket/   CheckoutRepository, CheckoutViewModel
  feature/auth/     AuthRepository, SupabaseAuthClient, ViewModels
  feature/orders/   OrdersRepository, OrdersViewModels
  ui/               Compose theme, navigation, placeholder screens
  util/             StripeCheckoutTabs
```

## Phase 2 — Catalog UI (done)

1. Start the web API on your PC: in `cursor-my-web-app`, run `npm start` (default port **3000**).
2. Android Studio → Run **debug** on an emulator (`API_BASE_URL` = `http://10.0.2.2:3000`).
3. **Home:** up to 6 products from `/api/products`; tap **View all products** → Products tab.
4. **Products:** full grid; **Add to basket** merges lines by product id (DataStore, same shape as web `basket`).
5. **Basket:** change quantity, remove lines, see USD subtotal.
6. If the API is down, Products/Home show a retry screen mentioning `npm start`.

Physical device: set debug `API_BASE_URL` to your machine LAN IP (e.g. `http://192.168.1.10:3000`) in `app/build.gradle.kts`.

## Phase 3 — Auth & account (done)

1. Start the web API: `npm start` in `cursor-my-web-app` (port **3000**).
2. Run the app on the emulator (`http://10.0.2.2:3000`).
3. **Account** tab (logged out): **Log in** / **Sign up**.
4. **Sign up:** person or company; required fields match `POST /api/users` (see `lib/schemas/auth.js`).
5. **Log in:** email + password → JWT stored in EncryptedSharedPreferences.
6. Logged-in **Account:** name/email from `GET /api/me`, **Profile**, **Log out**.
7. **Profile:** edit and **Save** (`PATCH /api/me`); API validation errors shown inline.
8. **Forgot password:** email only; generic success message from API.
9. Wrong password / lockout (**423**) / rate limit (**429**) show on the login screen.

**Phase 3b:** Google OAuth on login screen (see below). Telegram not in Android v1. Reset password: use web reset link for now.

## Google sign-in (Supabase OAuth)

Matches the web flow: Supabase `signInWithOAuth` (Google) → Supabase session `access_token` → `POST /api/auth/token` → app JWT in `SessionStore`.

### 1. Local app config

Copy the same Supabase project as `cursor-my-web-app` into `local.properties` (gitignored):

```properties
SUPABASE_URL=https://YOUR_PROJECT_REF.supabase.co
SUPABASE_ANON_KEY=your_anon_public_key
```

Sync/rebuild after editing. Without these keys, the login screen hides **Continue with Google**.

### 2. Supabase Dashboard → Authentication → URL Configuration

Add this **Redirect URL** (exact):

```text
eslamielectric://auth-callback
```

Keep existing web URLs (e.g. `https://www.eslamielectric.com/auth-callback.html`, `http://localhost:3000/auth-callback.html`).

**Authentication → Providers → Google:** enabled with the same Google Cloud OAuth client as the web app.

### 3. Android deep link

The app registers `eslamielectric://auth-callback` in `AndroidManifest.xml`. OAuth opens in **Chrome Custom Tabs** (Supabase Auth plugin).

### 4. SHA-1 fingerprints (Google Cloud / optional Android client)

If you add an **Android** OAuth client in [Google Cloud Console](https://console.cloud.google.com/) (APIs & Services → Credentials), register SHA-1 for each signing key:

| Key | Command (project root) |
|-----|-------------------------|
| **Debug** | `keytool -list -v -alias androiddebugkey -keystore "%USERPROFILE%\.android\debug.keystore" -storepass android -keypass android` |
| **Release (upload keystore)** | `keytool -list -v -alias upload -keystore release/upload-keystore.jks` |

Copy the **SHA-1** line from the output into the Android OAuth client. Package name: `com.eslamielectric.android`.

Supabase Google provider typically uses the **Web** client; SHA-1 is still useful for Play Console and any native Google APIs later.

### 5. Test flow

1. `npm start` in `cursor-my-web-app` (emulator API: debug `http://10.0.2.2:3000`).
2. Rebuild the app with `SUPABASE_*` in `local.properties`.
3. Account → **Log in** → **Continue with Google** → complete sign-in in Custom Tab → app receives deep link → logged in.

Errors (cancelled OAuth, invalid token, missing config) show on the login screen.

## Phase 4 — Checkout (done)

1. Start the web API with Stripe **test** keys: `npm start` in `cursor-my-web-app` (port **3000**).
2. Run the app on the emulator (`http://10.0.2.2:3000`).
3. Add products to the basket → **Proceed to checkout**.
4. Choose **Delivery** or **Collection**; for delivery, enter a street address (≥ 5 characters).
5. **Guest:** fill name + email (and optional phone). **Logged in:** JWT is sent automatically; complete profile if API returns `PROFILE_INCOMPLETE` (link opens Profile).
6. Tap **Pay with Stripe** → Chrome Custom Tab opens Stripe Checkout.
7. Pay with test card **4242 4242 4242 4242**, any future expiry, any CVC.
8. Close Custom Tab / return to app → payment is verified (`POST /api/orders/confirm-by-session`, `GET /api/orders/by-session`); basket clears on success; result screen shows order number.
9. Cancel Stripe or use a declined test card → incomplete result screen; basket unchanged.

**Errors:** empty basket blocked; `SESSION_EXPIRED` → Account login; rate limit (**429**) shown inline.

## Phase 5 — Orders (done)

1. Start the web API: `npm start` in `cursor-my-web-app` (port **3000**).
2. Run the app on the emulator (`http://10.0.2.2:3000`).
3. **Logged in:** Account → **My orders** → list (number, date, total, status, fulfillment) → tap for detail.
4. **Pending order (logged in):** **Complete payment** opens Stripe Custom Tab; **Cancel order** confirms then refreshes.
5. **Logged out:** Account → **Track guest order** → email + order number (e.g. `ORD-…`) **or** tracking token from confirmation email.
6. Guest lookup by email shows detail; pay/cancel need the **tracking token** (same as web `order.html?token=…`).
7. Expired JWT on orders APIs → session cleared → log in again.

## Planned phases

| Phase | Work |
|-------|------|
| **2** | ✅ Catalog grid, home featured strip, basket lines |
| **3** | ✅ Login, signup, profile, forgot password |
| **4** | ✅ Checkout (fulfillment, guest/logged-in, Stripe Custom Tab, return handling) |
| **5** | ✅ Orders list, detail, guest lookup, pending resume/cancel |
| **6** | ✅ Locale FA/RTL, product detail, search/categories, basket badge, pull-to-refresh, guest order deep link |

## Phase 6 — Polish & web parity (done)

1. Start the web API: `npm start` in `cursor-my-web-app` (port **3000**).
2. Run the app on the emulator (`http://10.0.2.2:3000`).
3. **Language:** Account tab → **English** / **فارسی** toggle. UI mirrors to RTL; product names use `name_fa` when FA is selected; `values-fa/strings.xml` applied via locale config.
4. **Product detail:** Tap a product on Home or Products → detail screen (image, localized name/description, category, price, **Add to basket**).
5. **Products search & categories:** Products tab → search field filters the loaded catalog; horizontal category chips from `GET /api/categories` (fallback: derived from products).
6. **Basket badge:** Add items → Basket tab icon shows total item count.
7. **Pull-to-refresh:** Home, Products, and My orders lists support pull-down refresh.
8. **Guest order deep link:** Open `https://www.eslamielectric.com/order.html?token=YOUR_TOKEN` or `eslamielectric://order?token=YOUR_TOKEN` on the device → app opens guest order detail.

**Deferred:** push notifications, full offline cache, Play internal testing track setup.

## License

Proprietary — Eslami Electric.
