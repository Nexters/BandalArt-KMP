plugins {
    id("bandalart.kmp")
    id("bandalart.kmp.android")
    id("bandalart.kmp.ios")
}

android.namespace = "com.nexters.bandalart.core.common"

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.common)
            api(projects.core.database)
            api(projects.core.datastore)

            implementation(libs.kotlinx.datetime)
            implementation(libs.koin.core)
        }
    }
}
