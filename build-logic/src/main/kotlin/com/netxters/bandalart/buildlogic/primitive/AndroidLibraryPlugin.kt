package com.netxters.bandalart.buildlogic.primitive

import com.android.build.gradle.LibraryExtension
import org.gradle.kotlin.dsl.configure

internal class AndroidLibraryPlugin : BuildLogicPlugin({
    applyPlugins(Plugins.ANDROID_LIBRARY, Plugins.KOTLIN_ANDROID)

    extensions.configure<LibraryExtension> {
        configureAndroid(this)

        defaultConfig.apply {
            targetSdk = libs.versions.targetSdk.get().toInt()
        }
    }
})
