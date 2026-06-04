plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.24" apply false
    // Firebase Cloud Messaging — applied in :app. Plugin reads app/google-services.json (gitignored).
    id("com.google.gms.google-services") version "4.4.2" apply false
}
