plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.bolao.copa2026.ui"
    compileSdk = 34
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.8" }
}

dependencies {
    api(platform("androidx.compose:compose-bom:2024.02.00"))
    api("androidx.compose.ui:ui")
    api("androidx.compose.material3:material3")
    api("androidx.compose.material:material-icons-extended")
    api("androidx.compose.ui:ui-tooling-preview")
    debugApi("androidx.compose.ui:ui-tooling")
}
