package com.nexters.bandalart.buildlogic

import com.nexters.bandalart.buildlogic.configure.applyPlugins
import org.gradle.api.Plugin
import org.gradle.api.Project

class KmpFeaturePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            applyPlugins(
                "bandalart.kmp",
                "bandalart.kmp.android",
                "bandalart.kmp.ios",
                "bandalart.kmp.compose",
                // apply("bandalart.detekt"),
            )
        }
    }
}
