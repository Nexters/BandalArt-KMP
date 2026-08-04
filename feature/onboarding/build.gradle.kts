@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.bandalart.android.feature)
}

android {
    namespace = "com.nexters.bandalart.feature.onboarding"

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
        projects.feature.home,

        libs.lottie.compose,
        libs.timber,
    )

    testImplementation(libs.circuit.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.test.junit)
}
