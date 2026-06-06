plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.24" apply false
    // Firebase Cloud Messaging — applied in :app. Plugin reads app/google-services.json (gitignored).
    id("com.google.gms.google-services") version "4.4.2" apply false
    // Firebase Crashlytics — applied in :app only when google-services.json is present.
    id("com.google.firebase.crashlytics") version "3.0.2" apply false
}
