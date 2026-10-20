plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "io.moth.pokeidlemobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.moth.pokeidlemobile"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-experimental"
        ndk { abiFilters += setOf("arm64-v8a") }
    }
    buildTypes {
        release { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    // Pinned stable GeckoView for reproducible experiments.
    implementation("org.mozilla.geckoview:geckoview:158.0.20261007201254")
    testImplementation("junit:junit:4.13.2")
}
