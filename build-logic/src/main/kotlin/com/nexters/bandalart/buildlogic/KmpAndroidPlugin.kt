package com.nexters.bandalart.buildlogic

import com.nexters.bandalart.buildlogic.configure.Plugins
import com.nexters.bandalart.buildlogic.configure.android
import com.nexters.bandalart.buildlogic.configure.applyPlugins
import com.nexters.bandalart.buildlogic.configure.configureAndroid
import com.nexters.bandalart.buildlogic.configure.kotlin
import com.nexters.bandalart.buildlogic.configure.libraryAndroidOptions
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

internal class KmpAndroidPlugin : BuildLogicPlugin(
    {
        applyPlugins(Plugins.ANDROID_LIBRARY)

        kotlin {
            androidTarget {
                compilations.all {
                    libraryAndroidOptions {
                        compileTaskProvider.configure {
                            compilerOptions {
                                jvmTarget.set(JvmTarget.JVM_17)
                            }
                        }
                    }
                }
            }
        }

        android {
            configureAndroid()
        }
    },
)
