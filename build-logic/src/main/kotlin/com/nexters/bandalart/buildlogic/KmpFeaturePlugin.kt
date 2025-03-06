package com.nexters.bandalart.buildlogic

import com.nexters.bandalart.buildlogic.configure.applyPlugins

internal class KmpFeaturePlugin : BuildLogicPlugin(
    {
        applyPlugins(
            "bandalart.kmp",
            "bandalart.kmp.android",
            "bandalart.kmp.ios",
            "bandalart.kmp.compose",
            "bandalart.detekt",
        )
    },
)
