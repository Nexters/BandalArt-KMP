package com.nexters.bandalart.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.nexters.bandalart.buildlogic.configure.applyPlugins
import com.nexters.bandalart.buildlogic.configure.compose
import com.nexters.bandalart.buildlogic.configure.kotlin
import com.nexters.bandalart.buildlogic.configure.libs
import org.gradle.kotlin.dsl.withType

internal class KmpComposePlugin : BuildLogicPlugin(
    {
        applyPlugins(
            "org.jetbrains.compose",
            "org.jetbrains.kotlin.plugin.compose",
        )

        kotlin {
            targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
                androidResources.enable = true
            }

            with(sourceSets) {
                commonMain.dependencies {
                    implementation(compose.dependencies.runtime)
                    implementation(compose.dependencies.foundation)
                    implementation(compose.dependencies.material3)
                    implementation(libs.compose.material.iconsCore)
                    implementation(libs.compose.ui.toolingPreview)
                    implementation(compose.dependencies.ui)
                    implementation(compose.dependencies.components.resources)
                }

                find { it.name == "androidMain" }?.apply {
                    dependencies {
                        implementation(compose.dependencies.preview)
                        implementation(compose.dependencies.uiTooling)
                    }
                }
            }
        }
    },
)
