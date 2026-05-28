# Play Store screenshots — Eslami Electric (Android)

Phone screenshots for the **Main store listing** in Google Play Console. Do not upload from this doc alone; use the PNG files under `phone/`.

## Files (`phone/`)

| File | Screen | Tab selected |
|------|--------|--------------|
| `01-home-featured.png` | Home — featured product grid, “View all products” | Home |
| `02-products-search.png` | Products — search field, category chips, catalog grid | Products |
| `03-basket-items.png` | Basket — line items, quantity controls, total, “Proceed to checkout” | Basket (badge: 3) |
| `04-account-sign-in.png` | Account — language toggle (EN/FA), guest track, Log in, Sign up | Account |

**Dimensions:** 1080×1920 PNG (9:16 phone aspect ratio).

## How these were produced

| Method | Status |
|--------|--------|
| **adb** (emulator or USB device) | Not used — `adb devices` showed no connected device at generation time. |
| **Generated mockups** | Used — `python scripts/generate-phone-screenshots.py` (Pillow). Styled to match the Compose app: Material 3 cards, slate/amber brand (same as eslamielectric.com), bottom nav (Home / Products / Basket / Account), and strings from `app/src/main/res/values/strings.xml`. |

Regenerate mockups:

```powershell
cd c:\Users\44741\Desktop\eslami-electric-android
pip install Pillow   # if needed
python scripts/generate-phone-screenshots.py
```

## Re-capture from emulator (real app UI)

Preferred for production listing once a device is available.

1. **Device:** Android Studio → **Device Manager** → create/start a phone AVD (e.g. Pixel 6).
2. **Catalog API:** From `cursor-my-web-app`, run `npm start` (or use production) so the catalog is not empty.
3. **Install app:**
   ```powershell
   cd c:\Users\44741\Desktop\eslami-electric-android
   .\gradlew.bat installDebug
   ```
4. **Navigate** to Home, Products, Basket (with items), Account.
5. **Capture:** Emulator toolbar → **camera** icon, or:
   ```powershell
   adb exec-out screencap -p > play-store\screenshots\phone\01-home-featured.png
   ```
6. Crop/resize to **1080×1920** if the emulator resolution differs.

### adb checklist

```powershell
adb devices          # must list one device
adb shell screencap -p /sdcard/ee-screen.png
adb pull /sdcard/ee-screen.png play-store\screenshots\phone\01-home-featured.png
```

## Play Console

- Upload under **Store presence → Main store listing → Phone screenshots** (at least 4 images).
- Optional: FA/RTL screenshots with locale set to فارسی in the app before capture.
