package com.nexters.bandalart.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class KmpFeaturePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("bandalart.kmp")
                apply("bandalart.kmp.android")
                apply("bandalart.kmp.ios")
                apply("bandalart.kmp.compose")
                // apply("bandalart.detekt")
            }
        }
    }
}
