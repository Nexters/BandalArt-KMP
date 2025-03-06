plugins {
    id("bandalart.kmp.feature")
    id("bandalart.android.feature")
    id("bandalart.kotlin.serialization")
}

android.namespace = "com.nexters.bandalart.feature.onboarding"

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
