package com.netxters.bandalart.android.convention

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureAndroid(extension: CommonExtension) {
    extension.apply {
        compileSdk = libs.versions.compileSdk.get().toInt()

        defaultConfig.apply {
            minSdk = libs.versions.minSdk.get().toInt()
        }

        compileOptions.apply {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
            isCoreLibraryDesugaringEnabled = true
        }

        dependencies {
            coreLibraryDesugaring(libs.desugar.jdk.libs)
            detektPlugins(libs.detekt.formatting)
        }
    }
}
