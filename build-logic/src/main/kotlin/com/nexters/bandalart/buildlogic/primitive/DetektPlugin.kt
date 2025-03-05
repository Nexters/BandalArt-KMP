package com.nexters.bandalart.buildlogic.primitive

import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class DetektPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("io.gitlab.arturbosch.detekt")

            configureDetekt(extensions.getByType<DetektExtension>())

            dependencies {
                "detektPlugins"(libs.findLibrary("detektFormatting").get())
                "detektPlugins"(libs.findLibrary("twitterComposeRule").get())
            }
        }
    }
}
