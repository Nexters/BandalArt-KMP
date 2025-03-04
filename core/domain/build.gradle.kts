plugins {
    id("bandalart.kmp")
    id("bandalart.kmp.android")
    id("bandalart.kmp.ios")
}

android.namespace = "com.nexters.bandalart.core.domain"

kotlin {
    sourceSets {
        commonMain.dependencies {
        }
    }
}
