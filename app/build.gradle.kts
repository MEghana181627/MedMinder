plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)

    // Required for Room with AGP 9
    id("com.android.legacy-kapt")
}

android {
    namespace = "uk.ac.tees.mad.s3640901"
    compileSdk = 37

    defaultConfig {
        applicationId = "uk.ac.tees.mad.s3640901"
        minSdk = 24
        targetSdk = 37

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Compose compiler/runtime must use one BOM
    val composeBom =
        platform("androidx.compose:compose-bom:2026.02.01")

    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Compose
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.runtime:runtime-saveable")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.foundation:foundation-layout")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-text")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Activity and navigation
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.activity:activity-ktx:1.13.0")
    implementation("androidx.navigation:navigation-compose:2.9.0")
    implementation("androidx.fragment:fragment-ktx:1.9.1")

    // Android and biometric support
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.biometric:biometric:1.1.0")

    // Room
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    add("kapt", "androidx.room:room-compiler:2.8.4")

    // OpenFDA API
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // Tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    // Debug tools
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}