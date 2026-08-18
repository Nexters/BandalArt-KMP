plugins {
    id("bandalart.kmp.feature")
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

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.designsystem)
            implementation(projects.core.domain)
            implementation(projects.core.navigation)
            implementation(projects.feature.home)
            implementation(projects.feature.onboarding)

            implementation(libs.circuit.runtime)
            implementation(libs.circuit.runtime.presenter)
            implementation(libs.circuit.runtime.ui)
            implementation(libs.kotlinx.coroutines.core)
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
