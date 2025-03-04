plugins {
    id("bandalart.kmp")
    id("bandalart.kmp.android")
    id("bandalart.kmp.ios")
    id("bandalart.kmp.compose")
}

android.namespace = "com.nexters.bandalart.core.ui"

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.designsystem)
            api(projects.core.model)

            implementation(libs.coil.compose)
            implementation(libs.coil.ktor)

            implementation(libs.ktor.client)
            implementation(libs.ktor.client.auth)
        }
    }
}
