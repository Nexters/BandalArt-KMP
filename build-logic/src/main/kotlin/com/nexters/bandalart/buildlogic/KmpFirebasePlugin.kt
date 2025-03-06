package com.nexters.bandalart.buildlogic

import com.nexters.bandalart.buildlogic.configure.Plugins
import com.nexters.bandalart.buildlogic.configure.applyPlugins
import com.nexters.bandalart.buildlogic.configure.kotlinMultiPlatform
import com.nexters.bandalart.buildlogic.configure.libs

internal class KmpFirebasePlugin : BuildLogicPlugin(
    {
        applyPlugins(Plugins.GOOGLE_SERVICES, Plugins.FIREBASE_CRASHLYTICS)

        kotlinMultiPlatform {
            with(sourceSets) {
                commonMain.dependencies {
                    implementation(libs.kmp.firebase.analytics)
                    implementation(libs.kmp.firebase.crashlytics)
                }

                androidMain.dependencies {
                    implementation(libs.firebase.common)
                }
            }
        }
    },
)
