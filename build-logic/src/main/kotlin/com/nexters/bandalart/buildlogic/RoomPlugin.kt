package com.nexters.bandalart.buildlogic

import com.nexters.bandalart.buildlogic.configure.Plugins
import com.nexters.bandalart.buildlogic.configure.applyPlugins
import com.nexters.bandalart.buildlogic.configure.kotlin
import com.nexters.bandalart.buildlogic.configure.kspKmp
import com.nexters.bandalart.buildlogic.configure.libs
import com.nexters.bandalart.buildlogic.configure.room
import org.gradle.kotlin.dsl.dependencies

internal class RoomPlugin : BuildLogicPlugin(
    {
        applyPlugins(Plugins.ANDROIDX_ROOM, Plugins.KSP)

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

        room {
            schemaDirectory("$projectDir/schemas")
        }
    },
)
