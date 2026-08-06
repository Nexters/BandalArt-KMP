plugins {
    id("bandalart.lint")
    id("bandalart.kmp")
    id("bandalart.kmp.android")
    id("bandalart.kmp.ios")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)

            compileOnly(libs.compose.stable.marker)
        }

        androidHostTest.dependencies {
            implementation(libs.bundles.android.unit.test)
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
