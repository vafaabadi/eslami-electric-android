import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    jacoco
}

import org.gradle.testing.jacoco.tasks.JacocoReport

// google-services.json is gitignored (contains the Firebase project id + API key for the Android app).
// CI builds without it must still compile — apply the plugin only when the file exists. Without it,
// FirebaseApp.initializeApp() returns null and the app skips token registration cleanly.
val googleServicesFile = rootProject.file("app/google-services.json")
val hasGoogleServices = googleServicesFile.exists()
if (hasGoogleServices) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
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

fun resolveApiBaseUrl(forDebug: Boolean): String {
    val override = sequenceOf(
        (project.findProperty("apiBaseUrl") as String?)?.trim(),
        System.getenv("API_BASE_URL")?.trim()
    ).firstOrNull { !it.isNullOrBlank() }
    return when {
        !override.isNullOrBlank() -> override
        forDebug -> "http://10.0.2.2:3000"
        else -> "https://www.eslamielectric.com"
    }
}

// CI passes -PversionCode=… (play-internal.yml: run_number + 100); local builds use default below.
// Manual Play uploads must exceed the highest versionCode on internal track (124 live as of 2026-06-08; use 125+ for manual uploads after CI).
val versionCodeOverride = (project.findProperty("versionCode") as String?)?.toIntOrNull()

android {
    namespace = "com.eslamielectric.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.eslamielectric.android"
        minSdk = 26
        targetSdk = 35
        versionCode = versionCodeOverride ?: 125
        versionName = "1.0.16"

        buildConfigField("String", "API_BASE_URL", buildConfigString(resolveApiBaseUrl(forDebug = false)))
        buildConfigField("String", "SUPABASE_URL", buildConfigString(supabaseUrl))
        buildConfigField("String", "SUPABASE_ANON_KEY", buildConfigString(supabaseAnonKey))
        buildConfigField("boolean", "FCM_CONFIGURED", hasGoogleServices.toString())
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
            enableUnitTestCoverage = true
            buildConfigField(
                "String",
                "API_BASE_URL",
                buildConfigString(resolveApiBaseUrl(forDebug = true))
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
                buildConfigString(resolveApiBaseUrl(forDebug = false))
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

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
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

    // Firebase Cloud Messaging — push notifications. Uses the BoM to keep transitive deps aligned.
    // FirebaseMessaging is safe to call regardless of google-services.json presence; without it,
    // FirebaseApp.initializeApp() returns null and FirebaseMessaging.getInstance() throws — handled
    // defensively in PushTokenManager.
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    // Task.await() bridge for Firebase Task → coroutine suspend functions.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Google Play In-App Review — no-op on emulator / non-Play installs; handled in ReviewPromptManager.
    implementation("com.google.android.play:review-ktx:2.0.2")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core-ktx:1.6.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.2")
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    group = "verification"
    description = "Unit test coverage report (HTML/XML). Package breakdown in HTML index."

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    val excludes = listOf(
        "**/R.class",
        "**/R\$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
        "**/*\$Lambda*.*",
        "**/*\$inlined*.*",
        "**/databinding/**",
        "**/generated/**"
    )

    val debugTree = fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
        exclude(excludes)
    }
    val mainSrc = "${project.projectDir}/src/main/java"

    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(
        fileTree(layout.buildDirectory.get()) {
            include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
        }
    )
}

/** Prints instruction coverage % for core/ and feature/ packages after jacocoTestReport. */
tasks.register("jacocoCoreFeatureSummary") {
    dependsOn("jacocoTestReport")
    group = "verification"
    doLast {
        val reportDir = layout.buildDirectory.dir("reports/jacoco/jacocoTestReport").get().asFile
        val htmlIndex = reportDir.resolve("html/index.html")
        if (!htmlIndex.exists()) {
            logger.lifecycle("JaCoCo HTML report not found at ${htmlIndex.path}")
            return@doLast
        }
        val text = htmlIndex.readText()
        val rowRegex = Regex(
            """<a href="([^"]+)" class="el_package">([^<]+)</a></td><td class="bar"[^>]*>.*?<td class="ctr2"[^>]*>(\d+)%</td>""",
            RegexOption.DOT_MATCHES_ALL
        )
        val totalRegex = Regex("""Total</td><td class="bar">([\d,]+) of ([\d,]+)""")
        var coreCovered = 0
        var coreTotal = 0
        var featureCovered = 0
        var featureTotal = 0
        rowRegex.findAll(text).forEach { match ->
            val pkg = match.groupValues[2]
            val detail = reportDir.resolve("html/${match.groupValues[1]}")
            if (!detail.exists()) return@forEach
            val totals = totalRegex.find(detail.readText()) ?: return@forEach
            fun parseCount(raw: String) = raw.replace(",", "").toIntOrNull()
            val missed = parseCount(totals.groupValues[1]) ?: return@forEach
            val total = parseCount(totals.groupValues[2]) ?: return@forEach
            val covered = (total - missed).coerceAtLeast(0)
            when {
                pkg.startsWith("com.eslamielectric.android.core") -> {
                    coreCovered += covered
                    coreTotal += total
                }
                pkg.startsWith("com.eslamielectric.android.feature") -> {
                    featureCovered += covered
                    featureTotal += total
                }
            }
        }
        fun pct(covered: Int, total: Int) =
            if (total == 0) "n/a" else "${(covered * 100.0 / total).toInt()}%"
        val corePct = if (coreTotal == 0) 0 else (coreCovered * 100.0 / coreTotal)
        val featurePct = if (featureTotal == 0) 0 else (featureCovered * 100.0 / featureTotal)
        val combinedTotal = coreTotal + featureTotal
        val combinedPct = if (combinedTotal == 0) 0.0 else (coreCovered + featureCovered) * 100.0 / combinedTotal
        logger.lifecycle(
            "JaCoCo instruction coverage — core/: ${pct(coreCovered, coreTotal)} | feature/: ${pct(featureCovered, featureTotal)} | combined: ${combinedPct.toInt()}%"
        )
        project.extensions.extraProperties.set("jacocoCorePct", corePct)
        project.extensions.extraProperties.set("jacocoFeaturePct", featurePct)
        project.extensions.extraProperties.set("jacocoCombinedPct", combinedPct)
    }
}

/**
 * Soft coverage gate: warns below 40% aspirational target; fails only below 20% floor.
 * Run locally: ./gradlew testDebugUnitTest jacocoTestReport jacocoCoverageGate
 */
tasks.register("jacocoCoverageGate") {
    dependsOn("jacocoCoreFeatureSummary")
    group = "verification"
    doLast {
        val combined = (project.extensions.extraProperties.get("jacocoCombinedPct") as? Double) ?: 0.0
        val floor = 20
        val target = 40
        logger.lifecycle("JaCoCo combined core+feature coverage: ${combined.toInt()}% (floor ${floor}%, target ${target}%)")
        if (combined < floor) {
            throw GradleException(
                "JaCoCo combined core+feature coverage ${combined.toInt()}% is below the ${floor}% floor."
            )
        }
        if (combined < target) {
            logger.warn(
                "JaCoCo combined core+feature coverage ${combined.toInt()}% is below the ${target}% aspirational target."
            )
        }
    }
}
