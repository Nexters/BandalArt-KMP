package com.nexters.bandalart.buildlogic

import com.android.build.gradle.LibraryExtension
import org.gradle.kotlin.dsl.configure

internal class AndroidLibraryComposePlugin : BuildLogicPlugin(
    {
        applyPlugins(Plugins.KOTLIN_COMPOSE)

        extensions.configure<LibraryExtension> {
            configureCompose(this)
        }
    },
)


