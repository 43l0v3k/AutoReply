plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            enableV1Signing = true
            enableV2Signing = true
        }
    }
    namespace = "com.example.autoreply"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.x43l0v3k.autoreply"
        minSdk = 24
        targetSdk = 36
        versionCode = 4
        versionName = "1.1.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
}
