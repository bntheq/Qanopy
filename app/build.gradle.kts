plugins {
    id("com.android.application") version "8.5.2"
    id("org.jetbrains.kotlin.android") version "1.9.24"
}

android {
    namespace = "io.github.bntheq.qanopy"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.github.bntheq.qanopy"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "X0-test"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
