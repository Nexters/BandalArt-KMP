plugins {
    id("bandalart.kmp")
    id("bandalart.kmp.android")
    id("bandalart.kmp.ios")
}

android.namespace = "com.nexters.bandalart.core.ui"

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.datastore.core)
            api(projects.core.model)

            implementation(libs.kotlinx.datetime)
        }
    }
}
