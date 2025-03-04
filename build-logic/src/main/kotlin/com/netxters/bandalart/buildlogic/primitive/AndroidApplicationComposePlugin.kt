package com.netxters.bandalart.buildlogic.primitive

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.kotlin.dsl.configure

internal class AndroidApplicationComposePlugin : BuildLogicPlugin(
    {
        applyPlugins(Plugins.ANDROID_APPLICATION, Plugins.KOTLIN_COMPOSE)

        extensions.configure<ApplicationExtension> {
            configureCompose(this)
        }
    },
)
