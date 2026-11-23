plugins {
    id("com.android.application") version "9.5.2"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21"
}

val ksPath: String? = System.getenv("QANOPY_KEYSTORE_PATH")

android {
    namespace = "io.github.bntheq.qanopy"
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        applicationId = "io.github.bntheq.qanopy"
        minSdk = 29
        targetSdk = 34
        versionCode = 51
        versionName = "X5.8part6"
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
        getByName("release") {
            isMinifyEnabled = true
            if (ksPath != null) {
                signingConfig = signingConfigs.getByName("qanopy")
            }
        }
        getByName("debug") {
            if (ksPath != null) {
                signingConfig = signingConfigs.getByName("qanopy")
            }
        }
    }

    lint {
        checkReleaseBuilds = false
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.11.01"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
}
