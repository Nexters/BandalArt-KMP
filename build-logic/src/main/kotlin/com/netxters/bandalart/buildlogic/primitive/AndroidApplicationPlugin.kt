package com.netxters.bandalart.buildlogic.primitive

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.kotlin.dsl.configure

internal class AndroidApplicationPlugin : BuildLogicPlugin(
    {
        applyPlugins(Plugins.ANDROID_APPLICATION, Plugins.KOTLIN_ANDROID)

        extensions.configure<ApplicationExtension> {
            configureAndroid(this)

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
