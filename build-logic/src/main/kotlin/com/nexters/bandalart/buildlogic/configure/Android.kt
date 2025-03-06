package com.nexters.bandalart.buildlogic.configure

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
// import com.nexters.bandalart.buildlogic.configure.detektPlugins

internal fun Project.configureAndroid(extension: CommonExtension<*, *, *, *, *, *>) {
    extension.apply {
        namespace?.let {
            this.namespace = it
        }

        compileSdk = libs.versions.compileSdk.get().toInt()

        defaultConfig {
            minSdk = libs.versions.minSdk.get().toInt()
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
            isCoreLibraryDesugaringEnabled = true
        }

        extensions.configure<KotlinProjectExtension> {
            jvmToolchain(17)
        }

        dependencies {
            coreLibraryDesugaring(libs.desugar.jdk.libs)
            // detektPlugins(libs.detekt.formatting)
        }
    }
}
