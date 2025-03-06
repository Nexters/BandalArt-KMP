package com.nexters.bandalart.buildlogic

import com.nexters.bandalart.buildlogic.configure.android
import com.nexters.bandalart.buildlogic.configure.applyPlugins
import com.nexters.bandalart.buildlogic.configure.compose
import com.nexters.bandalart.buildlogic.configure.kotlin
import org.gradle.api.Plugin
import org.gradle.api.Project

class KmpComposePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            applyPlugins(
                "org.jetbrains.compose",
                "org.jetbrains.kotlin.plugin.compose",
            )

            if (plugins.hasPlugin("com.android.library")) {
                android {
                    buildFeatures.compose = true
                }
            }

            kotlin {
                with(sourceSets) {
                    getByName("commonMain").apply {
                        dependencies {
                            implementation(compose.dependencies.runtime)
                            implementation(compose.dependencies.foundation)
                            implementation(compose.dependencies.material3)
                            implementation(compose.dependencies.ui)
                            implementation(compose.dependencies.components.resources)
                            implementation(compose.dependencies.components.uiToolingPreview)
                        }
                    }
                    find { it.name == "androidMain" }?.apply {
                        dependencies {
                            implementation(compose.dependencies.preview)
                        }
                    }
                }
            }
        }
    }
}
