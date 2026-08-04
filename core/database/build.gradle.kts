plugins {
    id("bandalart.lint")
    id("bandalart.kmp")
    id("bandalart.kmp.android")
    id("bandalart.kmp.ios")
    id("bandalart.room")
    id("bandalart.kotlin.serialization")
}

kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(libs.koin.android)
        }

        androidHostTest.dependencies {
            implementation(libs.bundles.android.unit.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.robolectric)
            implementation(libs.robolectric.junit5.extension)
            implementation(libs.turbine)
        }

        commonMain.dependencies {
            api(libs.koin.core)
        }
    }

    compilerOptions.freeCompilerArgs.add("-Xexpect-actual-classes")
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("-Djunit.platform.launcher.interceptors.enabled=true")
}
