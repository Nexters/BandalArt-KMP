package com.nexters.bandalart.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.nexters.bandalart.buildlogic.configure.Plugins
import com.nexters.bandalart.buildlogic.configure.applyPlugins
import com.nexters.bandalart.buildlogic.configure.configureAndroid
import com.nexters.bandalart.buildlogic.configure.configureKmp
import com.nexters.bandalart.buildlogic.configure.libs
import org.gradle.kotlin.dsl.configure

internal class AndroidApplicationPlugin : BuildLogicPlugin(
    {
        applyPlugins(Plugins.ANDROID_APPLICATION)

        extensions.configure<ApplicationExtension> {
            configureAndroid()
            configureKmp()

            defaultConfig {
                applicationId = libs.versions.packageName.get()
                minSdk = libs.versions.minSdk.get().toInt()
                targetSdk = libs.versions.targetSdk.get().toInt()

                val major = libs.versions.majorVersion.get().toInt()
                val minor = libs.versions.minorVersion.get().toInt()
                val patch = libs.versions.patchVersion.get().toInt()

                versionCode = (major * 10000) + (minor * 100) + patch
                versionName = "$major.$minor.$patch"
            }
        }
    },
)
