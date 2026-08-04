@file:Suppress("UnstableApiUsage", "INLINE_FROM_HIGHER_PLATFORM")

plugins {
    alias(libs.plugins.bandalart.android.feature)
}

android {
    namespace = "com.nexters.bandalart.feature.home"

    buildFeatures {
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }
}

ksp {
    arg("circuit.codegen.mode", "hilt")
}

dependencies {
    implementations(
        projects.feature.complete,

        libs.kotlinx.collections.immutable,
        libs.kotlinx.datetime,

        libs.androidx.core,

        libs.app.update,
        libs.app.update.ktx,

        libs.lottie.compose,
        libs.facebook.shimmer,
        libs.timber,
    )

    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.circuit.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.test.junit)
    testImplementation(libs.test.robolectric)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
