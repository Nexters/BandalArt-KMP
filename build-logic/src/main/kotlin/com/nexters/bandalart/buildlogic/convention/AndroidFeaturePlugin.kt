package com.netxters.bandalart.buildlogic.convention

import com.nexters.bandalart.buildlogic.primitive.BuildLogicPlugin
import com.nexters.bandalart.buildlogic.primitive.Plugins.KOTLIN_PARCELIZE
import com.nexters.bandalart.buildlogic.primitive.applyPlugins
import com.nexters.bandalart.buildlogic.primitive.implementation
import com.nexters.bandalart.buildlogic.primitive.libs
import com.nexters.bandalart.buildlogic.primitive.project
import org.gradle.kotlin.dsl.dependencies

internal class AndroidFeaturePlugin : BuildLogicPlugin(
    {
        applyPlugins(
            "bandalart.android.library",
            "bandalart.android.library.compose",
            KOTLIN_PARCELIZE,
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
