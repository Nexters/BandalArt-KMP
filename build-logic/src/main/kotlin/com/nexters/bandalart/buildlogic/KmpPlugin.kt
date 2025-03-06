package com.nexters.bandalart.buildlogic

import com.nexters.bandalart.buildlogic.configure.applyPlugins
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.internal.builtins.StandardNames.FqNames.target
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal class KmpPlugin : BuildLogicPlugin(
    {
        applyPlugins("org.jetbrains.kotlin.multiplatform")

        tasks.withType(KotlinCompile::class.java) {
            compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
        }
    },
)
