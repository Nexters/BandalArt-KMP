package com.nexters.bandalart.buildlogic

import com.nexters.bandalart.buildlogic.configure.kotlin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.internal.builtins.StandardNames.FqNames.target

internal class KmpIosPlugin : BuildLogicPlugin(
    {
        kotlin {
            iosX64()
            iosArm64()
            iosSimulatorArm64()
        }
    },
)
