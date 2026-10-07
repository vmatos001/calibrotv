plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.kotlin.ksp)
}

android {
    namespace = "com.example.calibretv"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.calibrotv.app"
        minSdk = 23
        targetSdk = 34
        versionCode = 58
        versionName = "3.44"
        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("keystore/calibrotv.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            enableV1Signing = true
            enableV2Signing = true
        }
        getByName("debug") {
            storeFile = file("keystore/calibrotv.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = false
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
      jniLibs {
        pickFirsts.add("lib/arm64-v8a/libonnxruntime.so")
        pickFirsts.add("lib/armeabi-v7a/libonnxruntime.so")
        pickFirsts.add("lib/arm64-v8a/libsherpa-onnx-jni.so")
        pickFirsts.add("lib/armeabi-v7a/libsherpa-onnx-jni.so")
        pickFirsts.add("lib/arm64-v8a/libsherpa-onnx-c-api.so")
        pickFirsts.add("lib/armeabi-v7a/libsherpa-onnx-c-api.so")
        pickFirsts.add("lib/arm64-v8a/libsherpa-onnx-cxx-api.so")
        pickFirsts.add("lib/armeabi-v7a/libsherpa-onnx-cxx-api.so")
      }
    }
}

kotlin {
    jvmToolchain(17)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation("androidx.compose.material:material-icons-extended")
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Room (SQLite local database)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)

  // EncryptedSharedPreferences (seguridad)
  implementation(libs.androidx.security.crypto)

  // Networking (OkHttp para descargas OTA fiables y rápidas)
  implementation("com.squareup.okhttp3:okhttp:4.12.0")

  // QR Code Generator (ZXing core estándar para códigos QR escaneables)
  implementation("com.google.zxing:core:3.5.3")


  // Desugaring de APIs modernas para compatibilidad con Fire OS / Android 7.1 (API 23+)
  coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

  // Apache Commons Compress v1.21 (100% compatible con Android API 21+ sin requerir java.nio.file)
  implementation("org.apache.commons:commons-compress:1.21")
}
