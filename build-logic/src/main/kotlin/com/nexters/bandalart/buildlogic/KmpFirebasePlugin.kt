package com.nexters.bandalart.buildlogic

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
}
