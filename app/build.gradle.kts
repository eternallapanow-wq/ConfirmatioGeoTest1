plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "pl.confirmatio.geotest"
    compileSdk = 35

    defaultConfig {
        applicationId = "pl.confirmatio.geotest"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1-test"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.gms:play-services-location:21.4.0")
}
