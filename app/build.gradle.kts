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
        versionCode = 18
        versionName = "X3.3c"
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

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
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
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
}
