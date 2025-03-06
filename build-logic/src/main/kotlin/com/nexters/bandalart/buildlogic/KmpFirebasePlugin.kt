package com.nexters.bandalart.buildlogic

import com.nexters.bandalart.buildlogic.configure.Plugins
import com.nexters.bandalart.buildlogic.configure.applyPlugins
import com.nexters.bandalart.buildlogic.configure.kotlinMultiPlatform
import com.nexters.bandalart.buildlogic.configure.libs
import org.gradle.api.Plugin
import org.gradle.api.Project

class KmpFirebasePlugin: Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            applyPlugins(Plugins.GOOGLE_SERVICES, Plugins.FIREBASE_CRASHLYTICS)

            kotlinMultiPlatform {
                with(sourceSets) {
                    commonMain {
                        dependencies {
                            implementation(libs.kmp.firebase.analytics)
                            implementation(libs.kmp.firebase.crashlytics)
                        }
                    }
                    androidMain {
                        dependencies {
                            implementation(libs.firebase.common)
                        }
                    }
                }
            }
        }
    }
}
