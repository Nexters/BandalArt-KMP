@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.bandalart.android.library)
    alias(libs.plugins.bandalart.android.library.compose)
}

android {
    namespace = "com.nexters.bandalart.core.ui"

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementations(
        projects.core.designsystem,
        projects.core.common,

        libs.kotlinx.datetime,
        libs.lottie.compose,
        libs.bundles.androidx.compose,
    )
}
