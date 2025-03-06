plugins {
    id("bandalart.kmp")
    id("bandalart.kmp.android")
    id("bandalart.kmp.ios")
    id("bandalart.kotlin.serialization")
}

android.namespace = "com.nexters.bandalart.core.ui"

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.koin.core)

            api(libs.androidx.datastore)
            api(libs.androidx.datastore.preferences)
        }
    }
}
