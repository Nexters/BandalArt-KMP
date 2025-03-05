plugins {
    id("bandalart.kmp")
    id("bandalart.kmp.android")
    id("bandalart.kmp.ios")
}

android.namespace = "com.nexters.bandalart.core.navigation"

kotlin {
    sourceSets {
        commonMain.dependencies {
        }
    }
}
