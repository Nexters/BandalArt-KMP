package com.nexters.bandalart.buildlogic

import com.nexters.bandalart.buildlogic.configure.android
import com.nexters.bandalart.buildlogic.configure.configureAndroid
import com.nexters.bandalart.buildlogic.configure.kotlin
import com.nexters.bandalart.buildlogic.configure.libraryAndroidOptions
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

class KmpAndroidPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
            }

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
        }
    }
}
