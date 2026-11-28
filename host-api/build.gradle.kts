// The plugin API as the app defines it. Plugins compile against this module but never ship it:
// at runtime the app itself provides these classes.
plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.example.hostapi"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig { minSdk = 30 }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures { compose = true }
}

dependencies {
  compileOnly(platform(libs.androidx.compose.bom))
  compileOnly(libs.androidx.compose.ui)
  compileOnly(libs.androidx.compose.ui.graphics)
  compileOnly(libs.androidx.media3.exoplayer)
  compileOnly(libs.androidx.core.ktx)
  compileOnly(libs.kotlinx.coroutines.core)
}
