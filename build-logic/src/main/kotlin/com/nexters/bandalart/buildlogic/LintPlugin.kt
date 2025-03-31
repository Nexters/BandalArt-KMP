package com.nexters.bandalart.buildlogic

import com.nexters.bandalart.buildlogic.configure.applyPlugins

internal class LintPlugin : BuildLogicPlugin(
    {
        applyPlugins("bandalart.spotless", "bandalart.detekt")
    },
)
