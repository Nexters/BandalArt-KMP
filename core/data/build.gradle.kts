plugins {
    id("bandalart.kmp")
    id("bandalart.kmp.android")
    id("bandalart.kmp.ios")
}

android.namespace = "com.nexters.bandalart.core.common"

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.database)
            implementation(projects.core.datastore)

            implementation(libs.kotlinx.datetime)
            implementation(libs.koin.core)
        }
    }
}
