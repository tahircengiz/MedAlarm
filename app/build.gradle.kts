import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

/**
 * Release signing is driven entirely by environment variables so the keystore
 * never lives in the repo. CI (release.yml) decodes the base64 keystore secret
 * to a file and exports these vars:
 *   SIGNING_KEYSTORE_FILE      absolute path to the decoded .p12
 *   SIGNING_KEYSTORE_PASSWORD  store password
 *   SIGNING_KEY_ALIAS          key alias
 *   SIGNING_KEY_PASSWORD       key password (same as store for our PKCS12)
 *
 * Locally, you can instead drop a keystore.properties file at the repo root with
 * storeFile/storePassword/keyAlias/keyPassword. If neither is present, the
 * release build falls back to the debug signing identity so `assembleRelease`
 * still produces an installable APK for quick local checks (NOT for distribution).
 */
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) load(FileInputStream(keystorePropsFile))
}

fun signingEnv(name: String): String? =
    System.getenv(name)?.takeIf { it.isNotBlank() }

val hasReleaseSigning: Boolean =
    (signingEnv("SIGNING_KEYSTORE_FILE") != null && signingEnv("SIGNING_KEYSTORE_PASSWORD") != null) ||
        keystoreProps.getProperty("storeFile") != null

android {
    namespace = "com.medalarm.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.medalarm.app"
        minSdk = 26
        targetSdk = 35
        // Versioning policy:
        //   versionName follows semver — major.minor.patch + pre-release suffix
        //   versionCode is monotonically increasing — bump for every CI-released build
        //   The CI release process bumps versionCode automatically; do not edit
        //   versionCode manually except for major resets at a final release.
        versionCode = 4
        versionName = "0.1.0-beta3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        // Room schema export for migration testing
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
            arg("room.generateKotlin", "true")
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                val envFile = signingEnv("SIGNING_KEYSTORE_FILE")
                if (envFile != null) {
                    storeFile = file(envFile)
                    storePassword = signingEnv("SIGNING_KEYSTORE_PASSWORD")
                    keyAlias = signingEnv("SIGNING_KEY_ALIAS")
                    keyPassword = signingEnv("SIGNING_KEY_PASSWORD")
                } else {
                    storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                    storePassword = keystoreProps.getProperty("storePassword")
                    keyAlias = keystoreProps.getProperty("keyAlias")
                    keyPassword = keystoreProps.getProperty("keyPassword")
                }
                // PKCS12 keystore produced by openssl.
                storeType = "PKCS12"
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // R8 full mode is the default since AGP 8.x — keeps aggressive optimizations.
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Disable debugging in release for security.
            isDebuggable = false
            // Use the release identity when configured; otherwise fall back to debug
            // signing so a local `assembleRelease` still installs (not for distribution).
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "/META-INF/LICENSE",
                "/META-INF/LICENSE.txt",
                "/META-INF/license.txt",
                "/META-INF/NOTICE",
                "/META-INF/NOTICE.txt",
                "/META-INF/notice.txt",
                "/META-INF/ASL2.0",
                "META-INF/*.kotlin_module"
            )
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }

    lint {
        // Bloatware-free manifesto enforcement:
        // - any INTERNET permission, network state, or analytics-shaped lib should fail the build.
        // We rely primarily on the CI grep guard for ban enforcement, but turn lint into an error
        // for any HardcodedDebugMode / MissingPermission etc. to avoid release-time surprises.
        warningsAsErrors = false
        abortOnError = true
        checkReleaseBuilds = true
        // Disable the heavy "Internationalization" check noise for now; we localize manually.
        disable += setOf("MissingTranslation")
    }
}

dependencies {
    // AndroidX core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // WorkManager + Hilt integration
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Serialization (for backup JSON)
    implementation(libs.kotlinx.serialization.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Logging — debug-only sink (no cloud)
    implementation(libs.timber)

    // Unit tests
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.truth)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.work.testing)

    // Instrumented tests
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.testing)
    kspAndroidTest(libs.hilt.compiler)
    androidTestImplementation(libs.mockk.android)
}

// JUnit 5 for unit tests
tasks.withType<Test> {
    useJUnitPlatform()
}
