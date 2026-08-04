package com.nexters.bandalart.buildlogic

import com.android.build.api.dsl.LibraryExtension
import com.nexters.bandalart.buildlogic.configure.Plugins
import com.nexters.bandalart.buildlogic.configure.applyPlugins
import com.nexters.bandalart.buildlogic.configure.configureAndroid
import org.gradle.kotlin.dsl.configure

internal class AndroidLibraryPlugin : BuildLogicPlugin({
    applyPlugins(Plugins.ANDROID_LIBRARY)

    extensions.configure<LibraryExtension> {
        configureAndroid(this)
    }
})
