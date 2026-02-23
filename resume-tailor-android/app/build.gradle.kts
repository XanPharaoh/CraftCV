plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.resumetailor.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.resumetailor.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        // Upgrade secret — set via environment variable, never commit real values
        buildConfigField("String", "UPGRADE_SECRET", "\"${System.getenv("UPGRADE_SECRET") ?: ""}\"")

        // AdMob — replace with your real App ID before publishing
        // Test ID shown below; real one goes in local.properties or CI env
        val adMobAppId = project.findProperty("ADMOB_APP_ID") as? String
            ?: "ca-app-pub-3940256099942544~3347511713" // Google test App ID
        manifestPlaceholders["admobAppId"] = adMobAppId
    }

    buildTypes {
        debug {
            // Local testing — your PC's LAN IP
            buildConfigField("String", "BASE_URL", "\"http://192.168.18.164:8001\"")
        }
        release {
            // Production — replace with your deployed server URL before publishing
            buildConfigField("String", "BASE_URL", "\"https://api.craftcv.com\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
}

dependencies {
    // Compose BOM — single version for all Compose libs
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    // Core Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation") // FlowRow

    // Activity + ViewModel + Navigation
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Networking — Retrofit + OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Google Play Billing
    implementation("com.android.billingclient:billing-ktx:7.0.0")

    // Google Mobile Ads (rewarded ads for free-tier users)
    implementation("com.google.android.gms:play-services-ads:23.6.0")

    // Core KTX
    implementation("androidx.core:core-ktx:1.15.0")

    // Debug tooling
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
