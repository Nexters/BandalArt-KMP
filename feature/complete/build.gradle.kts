plugins {
    id("bandalart.kmp.feature")
    id("bandalart.android.feature")
    id("bandalart.kotlin.serialization")
}

android.namespace = "com.nexters.bandalart.feature.complete"

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)

            implementation(libs.coil3.compose)
            implementation(libs.landscapist.coil3)
            implementation(libs.landscapist.placeholder)

            implementation(libs.uri.kmp)
            implementation(libs.napier)
            implementation(libs.cmptoast)
        }
    }
}
