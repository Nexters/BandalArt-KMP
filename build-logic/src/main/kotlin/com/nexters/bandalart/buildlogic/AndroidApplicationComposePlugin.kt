package com.nexters.bandalart.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.nexters.bandalart.buildlogic.configure.Plugins
import com.nexters.bandalart.buildlogic.configure.applyPlugins
import com.nexters.bandalart.buildlogic.configure.configureCompose
import org.gradle.kotlin.dsl.configure

internal class AndroidApplicationComposePlugin : BuildLogicPlugin(
    {
        applyPlugins(Plugins.ANDROID_APPLICATION, Plugins.KOTLIN_COMPOSE)

        extensions.configure<ApplicationExtension> {
            configureCompose(this)
        }
    },
)
