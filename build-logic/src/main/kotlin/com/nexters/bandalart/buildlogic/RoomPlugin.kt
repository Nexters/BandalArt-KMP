package com.nexters.bandalart.buildlogic

import com.nexters.bandalart.buildlogic.configure.Plugins
import com.nexters.bandalart.buildlogic.configure.applyPlugins
import com.nexters.bandalart.buildlogic.configure.kotlin
import com.nexters.bandalart.buildlogic.configure.kspKmp
import com.nexters.bandalart.buildlogic.configure.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.internal.builtins.StandardNames.FqNames.target

internal class RoomPlugin : BuildLogicPlugin(
    {
        applyPlugins(Plugins.KSP)

        kotlin {
            with(sourceSets) {
                commonMain.dependencies {
                    implementation(libs.androidx.room.runtime)
                    implementation(libs.androidx.sqlite.bundled)
                }
            }
        }

        dependencies {
            kspKmp(libs.androidx.room.compiler)
        }
    },
)
