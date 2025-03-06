plugins {
    id("bandalart.kmp")
    id("bandalart.kmp.android")
    id("bandalart.kmp.ios")
    id("bandalart.room")
    id("bandalart.kotlin.serialization")
}

android.namespace = "com.nexters.bandalart.core.ui"

kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(libs.koin.android)
        }

        commonMain.dependencies {
            api(libs.koin.core)
        }
    }
}
