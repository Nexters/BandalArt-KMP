plugins {
    id("bandalart.kmp.feature")
    id("bandalart.android.feature")
    id("bandalart.kotlin.serialization")
}

android.namespace = "com.nexters.bandalart.feature.home"

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.collections.immutable)
            implementation(libs.kotlinx.datetime)

            implementation(libs.kotlinx.coroutines.core)

            implementation(libs.uri.kmp)
            implementation(libs.cmptoast)
        }
    }
}
