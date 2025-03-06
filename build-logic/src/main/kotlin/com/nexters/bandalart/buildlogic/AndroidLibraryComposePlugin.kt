package com.nexters.bandalart.buildlogic

import com.android.build.gradle.LibraryExtension
import com.nexters.bandalart.buildlogic.configure.Plugins
import com.nexters.bandalart.buildlogic.configure.applyPlugins
import com.nexters.bandalart.buildlogic.configure.configureCompose
import org.gradle.kotlin.dsl.configure

internal class AndroidLibraryComposePlugin : BuildLogicPlugin(
    {
        applyPlugins(Plugins.KOTLIN_COMPOSE)

        extensions.configure<LibraryExtension> {
            configureCompose(this)
        }
    },
)


