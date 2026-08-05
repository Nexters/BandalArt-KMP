plugins {
    id("bandalart.lint")
    id("bandalart.kmp")
    id("bandalart.kmp.android")
    id("bandalart.kmp.ios")
    id("bandalart.kmp.compose")
    id("bandalart.kotlin.serialization")
    id("org.jetbrains.kotlin.plugin.parcelize")
    alias(libs.plugins.metro)
}

metro {
    enableCircuitCodegen.set(true)
}

kotlin {
    targets.withType<com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget>().configureEach {
        compilerOptions.freeCompilerArgs.addAll(
            "-P",
            "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=com.nexters.bandalart.core.navigation.CommonParcelize",
        )
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "feature-splash"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.app.update)
            implementation(libs.app.update.ktx)
        }

        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.designsystem)
            implementation(projects.core.domain)
            implementation(projects.core.navigation)
            implementation(projects.core.ui)
            implementation(projects.feature.onboarding)
            implementation(projects.feature.home)

            implementation(libs.circuit.runtime)
            implementation(libs.circuit.runtime.presenter)
            implementation(libs.circuit.runtime.ui)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.napier)
        }

        androidHostTest.dependencies {
            implementation(libs.bundles.android.unit.test)
            implementation(libs.circuit.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
