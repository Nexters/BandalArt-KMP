package com.nexters.bandalart.buildlogic

import com.nexters.bandalart.buildlogic.configure.configureDetekt
import com.nexters.bandalart.buildlogic.configure.libs
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import com.nexters.bandalart.buildlogic.configure.detektPlugins
import org.gradle.kotlin.dsl.dependencies

class DetektPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("io.gitlab.arturbosch.detekt")

            configureDetekt(extensions.getByType<DetektExtension>())

            dependencies {
                detektPlugins(libs.detekt.formatting)
            }
        }
    }
}
