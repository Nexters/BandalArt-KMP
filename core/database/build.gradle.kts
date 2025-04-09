plugins {
    id("bandalart.lint")
    id("bandalart.kmp")
    id("bandalart.kmp.android")
    id("bandalart.kmp.ios")
    id("bandalart.room")
    id("bandalart.kotlin.serialization")
    alias(libs.plugins.junit5.robolectric.extension)
}

android.namespace = "com.nexters.bandalart.core.database"

android {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(libs.koin.android)
        }

        androidUnitTest.dependencies {
            implementation(libs.bundles.android.unit.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.robolectric)
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
}
