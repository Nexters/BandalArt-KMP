plugins {
    id("bandalart.kmp.feature")
    id("bandalart.kotlin.serialization")
}

android.namespace = "com.nexters.bandalart.feature.onboarding"

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.designsystem)
            implementation(projects.core.domain)
            implementation(projects.core.navigation)
            implementation(projects.core.ui)

            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.lifecycle.viewmodel)

            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.viewmodel.navigation)

            implementation(libs.kotlinx.coroutines.core)
        }

        androidUnitTest.dependencies {
            implementation(libs.bundles.android.unit.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
