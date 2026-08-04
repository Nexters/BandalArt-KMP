package com.nexters.bandalart.buildlogic

import com.nexters.bandalart.buildlogic.configure.kotlin
import com.nexters.bandalart.buildlogic.configure.libs

internal class KmpFirebasePlugin : BuildLogicPlugin(
    {
        val firebaseBom = dependencies.platform(libs.firebase.bom)

        kotlin {
            with(sourceSets) {
                commonMain.dependencies {
                    implementation(libs.kmp.firebase.analytics)
                    implementation(libs.kmp.firebase.crashlytics)
                    implementation(libs.kmp.firebase.config)
                }

                androidMain.dependencies {
                    implementation(firebaseBom)
                    implementation(libs.firebase.common)
                }
            }
        }
    },
)
