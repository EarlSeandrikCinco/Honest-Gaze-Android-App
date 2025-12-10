plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.honestgaze"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.honestgaze"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
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
    // Kotlin standard library
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")

    // AndroidX & Material
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.appcompat:appcompat:1.7.0")

    // CameraX
    implementation("androidx.camera:camera-core:1.3.2")
    implementation("androidx.camera:camera-camera2:1.3.2")
    implementation("androidx.camera:camera-lifecycle:1.3.2")
    implementation("androidx.camera:camera-view:1.3.2")
    implementation("androidx.camera:camera-video:1.3.2")

    // ML Kit Face Detection
    implementation("com.google.mlkit:face-detection:16.1.7")

    // Firebase
    implementation("com.google.firebase:firebase-database-ktx:20.3.1")
    implementation("com.google.firebase:firebase-storage-ktx:20.2.0")
    implementation("com.google.firebase:firebase-auth-ktx:22.1.0")

    // Splashscreen
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    implementation(libs.androidx.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit.junit)
}


apply(plugin = "com.google.gms.google-services")