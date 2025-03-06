package com.nexters.bandalart.buildlogic

import com.nexters.bandalart.buildlogic.BuildLogicPlugin
import com.nexters.bandalart.buildlogic.applyPlugins
import com.nexters.bandalart.buildlogic.implementation
import com.nexters.bandalart.buildlogic.libs
import com.nexters.bandalart.buildlogic.project
import org.gradle.kotlin.dsl.dependencies

internal class AndroidFeaturePlugin : BuildLogicPlugin(
    {
        applyPlugins(
            // "bandalart.android.library",
            // "bandalart.android.library.compose",
        )

        dependencies {
            implementation(project(path = ":core:common"))
            implementation(project(path = ":core:designsystem"))
            implementation(project(path = ":core:domain"))
            implementation(project(path = ":core:ui"))
            implementation(project(path = ":core:navigation"))

            implementation(libs.navigation.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.viewmodel.navigation)
        }
    },
)
