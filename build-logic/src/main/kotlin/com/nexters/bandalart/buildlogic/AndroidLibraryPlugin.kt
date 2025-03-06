package com.nexters.bandalart.buildlogic

import com.android.build.gradle.LibraryExtension
import com.nexters.bandalart.buildlogic.configure.Plugins
import com.nexters.bandalart.buildlogic.configure.applyPlugins
import com.nexters.bandalart.buildlogic.configure.configureAndroid
import com.nexters.bandalart.buildlogic.configure.libs
import org.gradle.kotlin.dsl.configure

internal class AndroidLibraryPlugin : BuildLogicPlugin({
    applyPlugins(Plugins.ANDROID_LIBRARY, Plugins.KOTLIN_ANDROID)

    extensions.configure<LibraryExtension> {
        configureAndroid(this)
        configureKmp()

        defaultConfig.apply {
            targetSdk = libs.versions.targetSdk.get().toInt()
        }
    }
})
