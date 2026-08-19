plugins {
    id("bandalart.kmp.feature")
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

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core)
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
            implementation(projects.feature.complete)

            implementation(libs.circuit.runtime)
            implementation(libs.circuit.runtime.presenter)
            implementation(libs.circuit.runtime.ui)
            implementation(libs.circuit.retained)

            implementation(libs.kotlinx.collections.immutable)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core)

            implementation(libs.uri.kmp)
            implementation(libs.cmptoast)
            implementation(libs.jindong.core)
            implementation(libs.jindong.compose)
            implementation(libs.napier)
        }

        androidHostTest.dependencies {
            implementation(libs.bundles.android.unit.test)
            implementation(libs.androidx.compose.ui.test)
            implementation(libs.circuit.test)
            implementation(libs.junit)
            implementation(libs.junit.vintage.engine)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.robolectric)
            implementation(libs.turbine)
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
