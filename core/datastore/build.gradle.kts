plugins {
    id("bandalart.lint")
    id("bandalart.kmp")
    id("bandalart.kmp.android")
    id("bandalart.kmp.ios")
    id("bandalart.kotlin.serialization")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.koin.core)

            implementation(libs.androidx.datastore)
            implementation(libs.androidx.datastore.preferences)
        }

        androidHostTest.dependencies {
            implementation(libs.bundles.android.unit.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }

    compilerOptions.freeCompilerArgs.add("-Xexpect-actual-classes")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
