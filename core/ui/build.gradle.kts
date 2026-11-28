plugins {
    id("bandalart.lint")
    id("bandalart.kmp")
    id("bandalart.kmp.android")
    id("bandalart.kmp.ios")
    id("bandalart.kmp.compose")
    id("bandalart.kotlin.serialization")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.designsystem)

            implementation(libs.compottie)
            implementation(libs.coil3.compose)
            implementation(libs.compose.hig)
            implementation(libs.compose.hig.adaptive)
            implementation(libs.compose.capsule)
        }

        androidHostTest.dependencies {
            implementation(libs.bundles.android.unit.test)
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
