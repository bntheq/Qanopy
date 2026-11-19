plugins {
    id("com.android.application") version "8.5.2"
    id("org.jetbrains.kotlin.android") version "1.9.24"
}

val ksPath: String? = System.getenv("QANOPY_KEYSTORE_PATH")

android {
    namespace = "io.github.bntheq.qanopy"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.github.bntheq.qanopy"
        minSdk = 29
        targetSdk = 34
        versionCode = 3
        versionName = "X0-imza2"
    }

    signingConfigs {
        if (ksPath != null) {
            create("qanopy") {
                storeFile = file(ksPath)
                storePassword = System.getenv("QANOPY_KEY_PASSWORD")
                keyAlias = "qanopy"
                keyPassword = System.getenv("QANOPY_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("debug") {
            if (ksPath != null) {
                signingConfig = signingConfigs.getByName("qanopy")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
