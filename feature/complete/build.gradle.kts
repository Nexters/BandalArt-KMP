plugins {
    id("template.convention.kmp")
    id("template.convention.kmp.android")
    id("template.convention.kmp.ios")
    id("template.convention.kmp.compose")
    id("template.convention.kotlin.serialization")
}

android.namespace = "com.nexters.bandalart.feature.splash"

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.ui)
            implementation(projects.core.common)
            implementation(projects.core.data)

            implementation(libs.navigation.compose)

            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.coroutines.core)

            implementation(libs.koin.core)

            implementation(libs.coil.compose)
        }
    }
}
