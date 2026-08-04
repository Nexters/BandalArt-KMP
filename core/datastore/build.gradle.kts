@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.bandalart.android.library)
    alias(libs.plugins.bandalart.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.nexters.bandalart.core.datastore"

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementations(
        libs.kotlinx.serialization.json,
        libs.androidx.datastore.preferences,
    )
}
