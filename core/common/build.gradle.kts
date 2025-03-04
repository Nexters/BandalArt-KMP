plugins {
    id("template.convention.kmp")
    id("template.convention.kmp.android")
    id("template.convention.kmp.ios")
}

android.namespace = "com.nexters.bandalart.core.common"

kotlin {
    sourceSets {
        commonMain.dependencies {
        }
    }
}
