@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.bandalart.android.feature)
}

android {
    namespace = "com.nexters.bandalart.feature.complete"

    buildFeatures {
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

ksp {
    arg("circuit.codegen.mode", "hilt")
}

dependencies {
    implementations(
        libs.lottie.compose,
        libs.timber,
        libs.coil.compose,

        libs.bundles.landscapist,
    )

    testImplementation(libs.circuit.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.test.junit)
    testImplementation(libs.test.robolectric)
}
