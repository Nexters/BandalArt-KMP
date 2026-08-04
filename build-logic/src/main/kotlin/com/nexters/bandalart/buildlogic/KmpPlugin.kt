package com.nexters.bandalart.buildlogic

import com.nexters.bandalart.buildlogic.configure.applyPlugins
import com.nexters.bandalart.buildlogic.configure.kotlin

internal class KmpPlugin : BuildLogicPlugin(
    {
        applyPlugins("org.jetbrains.kotlin.multiplatform")

        kotlin {
            jvmToolchain(17)
        }
    },
)
