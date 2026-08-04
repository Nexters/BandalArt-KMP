@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.bandalart.android.feature)
}

android {
    namespace = "com.nexters.bandalart.feature.splash"

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
        projects.feature.onboarding,

        libs.app.update,
        libs.app.update.ktx,

        libs.lottie.compose,
        libs.timber,
    )

    testImplementation(libs.circuit.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.test.junit)
}
