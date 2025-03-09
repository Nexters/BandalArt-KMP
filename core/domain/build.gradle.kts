plugins {
    id("bandalart.lint")
    id("bandalart.kmp")
    id("bandalart.kmp.android")
    id("bandalart.kmp.ios")
}

android.namespace = "com.nexters.bandalart.core.domain"

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)

            compileOnly(libs.compose.stable.marker)
        }
    }
}
