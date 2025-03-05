plugins {
    id("bandalart.kmp")
    id("bandalart.kmp.android")
    id("bandalart.kmp.ios")
    id("bandalart.room")
}

android.namespace = "com.nexters.bandalart.core.ui"

kotlin {
    sourceSets {
        commonMain.dependencies {
        }
    }
}
