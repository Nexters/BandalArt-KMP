package com.nexters.bandalart.buildlogic

import com.nexters.bandalart.buildlogic.configure.Plugins
import com.nexters.bandalart.buildlogic.configure.applyPlugins
import com.nexters.bandalart.buildlogic.configure.detektPlugins
import com.nexters.bandalart.buildlogic.configure.libs
import org.gradle.api.JavaVersion
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

internal class JvmKotlinPlugin : BuildLogicPlugin({
    applyPlugins(Plugins.JAVA_LIBRARY, Plugins.KOTLIN_JVM)

    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    extensions.configure<KotlinProjectExtension> {
        jvmToolchain(17)
    }

    dependencies {
        detektPlugins(libs.detekt.formatting)
    }
})
