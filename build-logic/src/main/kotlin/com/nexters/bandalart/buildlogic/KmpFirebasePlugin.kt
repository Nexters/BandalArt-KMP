package com.nexters.bandalart.buildlogic

import com.nexters.bandalart.buildlogic.configure.Plugins
import com.nexters.bandalart.buildlogic.configure.applyPlugins
import com.nexters.bandalart.buildlogic.configure.kotlin
import com.nexters.bandalart.buildlogic.configure.libs

internal class KmpFirebasePlugin : BuildLogicPlugin(
    {
        applyPlugins(Plugins.GOOGLE_SERVICES, Plugins.FIREBASE_CRASHLYTICS)

        kotlin {
            with(sourceSets) {
                commonMain.dependencies {
                    implementation(libs.kmp.firebase.analytics)
                    implementation(libs.kmp.firebase.crashlytics)
                    implementation(libs.kmp.firebase.config)
                }

                androidMain.dependencies {
                    implementation(libs.firebase.common)
                }
            }
        }
    },
)
