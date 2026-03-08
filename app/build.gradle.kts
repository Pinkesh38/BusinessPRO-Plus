plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.businessproplus"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.businessproplus"
        minSdk = 24
        targetSdk = 36
        versionCode = 8
        versionName = "1.7"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "MASTER_PIN", "\"1234\"")
    }

    androidResources {
        localeFilters += listOf("en", "hi", "gu")
    }

    buildTypes {
        release {
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

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/INDEX.LIST"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // UI and Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.fragment.ktx)
    
    // Architecture Components
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    
    // 🛡️ PAGING 3
    implementation(libs.androidx.paging.runtime)
    
    // HILT
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    
    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)
    
    // Database (Room)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging) // 🛡️ ROOM PAGING Integration
    
    // Google Drive & Auth
    implementation(libs.google.auth)
    implementation(libs.google.api.client)
    implementation(libs.google.drive)
    implementation(libs.google.http.gson)
    
    // Utils & External Libraries
    implementation(libs.glide)
    implementation(libs.androidx.biometric)
    implementation(libs.mpandroidchart)
    implementation(libs.lottie)
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
