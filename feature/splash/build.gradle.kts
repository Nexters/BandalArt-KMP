plugins {
    id("bandalart.kmp")
    id("bandalart.kmp.android")
    id("bandalart.kmp.ios")
    id("bandalart.kmp.compose")
    id("bandalart.android.feature")
    id("bandalart.kotlin.serialization")
}

android.namespace = "com.nexters.bandalart.feature.splash"

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
