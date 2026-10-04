plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.dlmaster"
    compileSdk = 34

    // 自定义签名配置 - 兼容 Android 5.0+
    signingConfigs {
        create("stable") {
            // 用 AGP 自带的 debug keystore
            storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = false
            enableV4Signing = false
        }
    }

    defaultConfig {
        applicationId = "com.dlmaster"
        minSdk = 21
        targetSdk = 34
        versionCode = 17
        versionName = "17.0.0"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("stable")
            // 关键:禁用 v3/v4
            packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
        }
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("stable")
        }
    }

    // 强制关闭 v3/v4(双重保险)
    androidComponents {
        onVariants(selector().all()) { variant ->
            variant.outputs.forEach { output ->
                // no-op,签名配置里已设
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.dynamicanimation:dynamicanimation:1.0.0")
    implementation("androidx.window:window:1.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
}
