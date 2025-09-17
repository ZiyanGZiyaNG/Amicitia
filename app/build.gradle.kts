plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // 若使用 Kotlin 2.x，建議保留這個 plugin；若是 Kotlin 1.9.x 就刪掉它改用 composeOptions
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.amicitia"
    compileSdk = 34 // 先用穩定版，等一切正常再升到 36 Preview

    defaultConfig {
        applicationId = "com.example.amicitia"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // 使用較新的 AGP 時，JDK 17 相容性最好
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }

    // 若你不是用 Kotlin 2.x（或移除了 kotlin.compose plugin），就加上這段：
    // composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
}

dependencies {
    // 用 BOM 管所有 Compose 版本，不要和個別套件混搭版本號
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))

    // Android 基礎
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.9.2")

    // Compose UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-text")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // 工具 / 測試
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    implementation("androidx.navigation:navigation-compose:2.7.7")
}