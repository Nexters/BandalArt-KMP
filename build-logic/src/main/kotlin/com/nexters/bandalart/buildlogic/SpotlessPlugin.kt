package com.nexters.bandalart.buildlogic

import com.diffplug.gradle.spotless.SpotlessExtension
import com.nexters.bandalart.buildlogic.configure.applyPlugins
import com.nexters.bandalart.buildlogic.configure.libs
import org.gradle.kotlin.dsl.configure

class SpotlessPlugin : BuildLogicPlugin(
    {
        applyPlugins("com.diffplug.spotless")
        val editorConfigFile = rootProject.file(".editorconfig")

        extensions.configure<SpotlessExtension> {
            providers.gradleProperty("spotlessRatchetFrom").orNull?.let(::ratchetFrom)

            kotlin {
                target("**/*.kt")
                targetExclude("**/build/**/*.kt")
                ktlint(libs.versions.ktlint.get()).setEditorConfigPath(editorConfigFile)
            }
            kotlinGradle {
                target("**/*.gradle.kts")
                targetExclude("**/build/**/*.gradle.kts")
                ktlint(libs.versions.ktlint.get()).setEditorConfigPath(editorConfigFile)
            }
        }
    },
)
