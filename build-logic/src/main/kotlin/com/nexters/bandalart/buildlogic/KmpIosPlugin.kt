package com.nexters.bandalart.buildlogic

import com.nexters.bandalart.buildlogic.configure.kotlin

internal class KmpIosPlugin : BuildLogicPlugin(
    {
        kotlin {
            iosArm64()
            iosSimulatorArm64()
        }
    },
)
