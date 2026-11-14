plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false

    id("com.android.legacy-kapt") version "9.4.1" apply false
}