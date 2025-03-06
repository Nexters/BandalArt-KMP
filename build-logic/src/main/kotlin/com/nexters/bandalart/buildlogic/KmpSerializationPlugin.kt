package com.nexters.bandalart.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class KotlinSerializationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.plugin.serialization")
            }

            kotlinMultiPlatform {
                with(sourceSets) {
                    commonMain {
                        dependencies {
                            implementation(libs.kotlinx.serialization.json)
                        }
                    }
                }
            }
        }
    }
}
