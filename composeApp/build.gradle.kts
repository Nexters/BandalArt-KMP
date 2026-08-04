plugins {
    id("bandalart.lint")
    id("bandalart.kmp")
    id("bandalart.kmp.android")
    id("bandalart.kmp.compose")
    id("bandalart.kmp.ios")
    id("bandalart.kmp.firebase")
}

kotlin {
    val xcfName = "ComposeApp"

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = xcfName
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.koin.android)
        }

        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.data)
            implementation(projects.core.database)
            implementation(projects.core.datastore)
            implementation(projects.core.designsystem)
            implementation(projects.core.domain)
            implementation(projects.core.navigation)
            implementation(projects.core.ui)

            implementation(projects.feature.complete)
            implementation(projects.feature.home)
            implementation(projects.feature.onboarding)
            implementation(projects.feature.splash)

            implementation(libs.androidx.navigation.compose)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)

            implementation(libs.cmptoast)
            implementation(libs.napier)
        }
    }

    compilerOptions.freeCompilerArgs.add("-Xexpect-actual-classes")
}
