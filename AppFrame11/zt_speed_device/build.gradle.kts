plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.trade.zt_speed_device"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.rynatsa.xtrendspeed"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // 替换为你的 Google Cloud 项目编号（Play Console → Play Integrity API 关联的 GCP 项目）
        buildConfigField("long", "PLAY_CLOUD_PROJECT_NUMBER", "0L")
    }

//    signingConfigs {
//        create("xtrendsf") {
//            keyAlias = "xtrendsf"
//            keyPassword = "xtrendsf"
//            storeFile = file("signing/XtrendSF.jks")
//            storePassword = "xtrendsf"
//        }
//    }

    buildTypes {
        debug {
//            signingConfig = signingConfigs.getByName("xtrendsf")
        }
        release {
            isMinifyEnabled = false
//            signingConfig = signingConfigs.getByName("xtrendsf")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.play.integrity)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
