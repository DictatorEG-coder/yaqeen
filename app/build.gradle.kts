plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.yaqeen.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.yaqeen.app"
        minSdk = 24
        targetSdk = 35
        versionCode = providers.gradleProperty("YAQEEN_VERSION_CODE").orElse("2").get().toInt()
        versionName = "0.2.0"
        val qfProxyBaseUrl = providers.gradleProperty("QF_PROXY_BASE_URL").orElse("").get()
        buildConfigField("String", "QF_PROXY_BASE_URL", "\"${qfProxyBaseUrl}\"")
    }

    signingConfigs {
        create("stableDebug") {
            storeFile = rootProject.file(".github/yaqeen-debug.keystore")
            storeType = "PKCS12"
            storePassword = "yaqeenDebugStore2026"
            keyAlias = "yaqeen-debug"
            keyPassword = "yaqeenDebugStore2026"
        }
    }

    buildTypes {
        getByName("debug") {
            val stableKey = rootProject.file(".github/yaqeen-debug.keystore")
            if (stableKey.exists()) signingConfig = signingConfigs.getByName("stableDebug")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    sourceSets {
        getByName("test").resources.srcDir("src/main/assets")
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
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.media3:media3-exoplayer:1.9.4")
    implementation("androidx.media3:media3-session:1.9.4")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
