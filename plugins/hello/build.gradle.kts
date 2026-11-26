// A plugin is an ordinary Android application module. Its APK is never installed on the phone: the
// Local Player app downloads it and loads its code. Everything the app already ships (Compose,
// Material3, Media3, coroutines...) is compileOnly here so it is NOT packaged a second time.
plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.example.plugin.hello"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.example.plugin.hello"
    minSdk = 30
    targetSdk = 36
    versionCode = 1
    versionName = "1.0.0"
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures { compose = true }
}

dependencies {
  compileOnly(project(":host-api"))

  // Provided by the app at runtime:
  compileOnly(platform(libs.androidx.compose.bom))
  compileOnly(libs.androidx.compose.ui)
  compileOnly(libs.androidx.compose.ui.graphics)
  compileOnly(libs.androidx.compose.material3)
  compileOnly(libs.androidx.compose.material.icons.core)
  compileOnly(libs.androidx.compose.material.icons.extended)
  compileOnly(libs.androidx.media3.exoplayer)
  compileOnly(libs.androidx.core.ktx)
  compileOnly(libs.kotlinx.coroutines.android)
  compileOnly(libs.kotlinx.coroutines.core)

  // Anything you add with implementation(...) is packaged inside the plugin file.
}
