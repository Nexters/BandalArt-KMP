package com.netxters.bandalart.buildlogic.primitive

import org.gradle.api.Plugin
import org.gradle.api.Project

class KmpFirebasePlugin: Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.google.firebase.crashlytics")
                apply("com.google.gms.google-services")

                kotlinMultiPlatform {
                    with(sourceSets) {
                        commonMain {
                            dependencies {
                                implementation(libs.findLibrary("kmp-firebase-analytics").get().get())
                                implementation(libs.findLibrary("kmp-firebase-crashlytics").get().get())
                                implementation(libs.findLibrary("kmp-firebase-config").get().get())
                            }
                        }
                        androidMain {
                            dependencies {
                                implementation(libs.findLibrary("firebase-common").get().get())
                            }
                        }
                    }
                }
            }
        }
    }
}
