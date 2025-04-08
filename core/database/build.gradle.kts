plugins {
    id("bandalart.lint")
    id("bandalart.kmp")
    id("bandalart.kmp.android")
    id("bandalart.kmp.ios")
    id("bandalart.room")
    id("bandalart.kotlin.serialization")
}

android.namespace = "com.nexters.bandalart.core.database"

kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(libs.koin.android)
        }

        androidUnitTest.dependencies {
            implementation(libs.bundles.android.unit.test)
            implementation(libs.kotlinx.coroutines.test)
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
