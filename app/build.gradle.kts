plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.greenhouse"
    // Tetap di 35 sesuai keinginan Anda
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.greenhouse"
        minSdk = 24
        targetSdk = 35
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Gunakan versi library yang stabil untuk SDK 35
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    
    // VERSI PENTING: Jangan gunakan 1.10.0 ke atas jika compileSdk masih 35
    implementation("androidx.activity:activity:1.9.3")
    implementation("androidx.core:core-ktx:1.13.0")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
