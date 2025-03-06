plugins {
    id("bandalart.kmp")
    id("bandalart.kmp.android")
    id("bandalart.kmp.ios")
    id("bandalart.kmp.compose")
}

android.namespace = "com.nexters.bandalart.core.common"

kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.core)
        }

        commonMain.dependencies {
            implementation(libs.androidx.lifecycle.runtime.compose)

            implementation(libs.kotlinx.datetime)
            implementation(libs.napier)
            implementation(libs.uri.kmp)
        }
    }

    compilerOptions.freeCompilerArgs.add("-Xexpect-actual-classes")
}
