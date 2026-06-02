import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

val localPropertiesFile = rootProject.file("local.properties")
val localProperties = Properties()
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

fun buildConfigString(value: String?): String {
    val escaped = (value ?: "").replace("\\", "\\\\").replace("\"", "\\\"")
    return "\"$escaped\""
}

fun resolveSupabaseProperty(name: String): String {
    return sequenceOf(
        localProperties.getProperty(name)?.trim(),
        (project.findProperty(name) as String?)?.trim(),
        System.getenv(name)?.trim()
    ).firstOrNull { !it.isNullOrBlank() }.orEmpty()
}

val supabaseUrl = resolveSupabaseProperty("SUPABASE_URL")
val supabaseAnonKey = resolveSupabaseProperty("SUPABASE_ANON_KEY")

// CI passes -PversionCode=… (play-internal.yml); local builds use default below.
val versionCodeOverride = (project.findProperty("versionCode") as String?)?.toIntOrNull()

android {
    namespace = "com.eslamielectric.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.eslamielectric.android"
        minSdk = 26
        targetSdk = 35
        versionCode = versionCodeOverride ?: 11
        versionName = "1.0.10"

        buildConfigField("String", "API_BASE_URL", "\"https://www.eslamielectric.com\"")
        buildConfigField("String", "SUPABASE_URL", buildConfigString(supabaseUrl))
        buildConfigField("String", "SUPABASE_ANON_KEY", buildConfigString(supabaseAnonKey))
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                keyAlias = keystoreProperties.getProperty("KEY_ALIAS")
                keyPassword = keystoreProperties.getProperty("KEY_PASSWORD")
                storeFile = rootProject.file(keystoreProperties.getProperty("STORE_FILE"))
                storePassword = keystoreProperties.getProperty("STORE_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"http://10.0.2.2:3000\""
            )
        }
        release {
            // Minify off for v1: Retrofit + kotlinx.serialization need soak testing with
            // proguard-rules.pro before enabling. Rules are ready for a later release.
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"https://www.eslamielectric.com\""
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            // Without keystore.properties: unsigned release AAB/APK (CI smoke tests only).
            // Play uploads need signing — copy keystore.properties.example and add upload keystore.
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation("androidx.browser:browser:1.8.0")
    implementation("io.coil-kt:coil-compose:2.6.0")

    val supabaseBom = platform("io.github.jan-tennert.supabase:bom:2.6.1")
    implementation(supabaseBom)
    implementation("io.github.jan-tennert.supabase:gotrue-kt")
    implementation("io.ktor:ktor-client-android:2.3.12")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
