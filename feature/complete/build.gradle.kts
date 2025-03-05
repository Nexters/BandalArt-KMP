plugins {
    id("bandalart.kmp")
    id("bandalart.kmp.android")
    id("bandalart.kmp.ios")
    id("bandalart.kmp.compose")
    id("bandalart.kotlin.serialization")
}

android.namespace = "com.nexters.bandalart.feature.complete"

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.ui)
            implementation(projects.core.common)

            implementation(libs.navigation.compose)

            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)

            implementation(libs.koin.core)
        }
    }
}
