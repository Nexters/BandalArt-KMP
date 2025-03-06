package com.nexters.bandalart.buildlogic.configure

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal val Project.libs
    get() = the<LibrariesForLibs>()

internal fun Project.applyPlugins(vararg plugins: String) {
    plugins.forEach(pluginManager::apply)
}

internal fun Project.kotlinMultiPlatform(action: KotlinMultiplatformExtension.() -> Unit) {
    extensions.configure(action)
}

internal val Project.isAndroidProject: Boolean
    get() = pluginManager.hasPlugin(Plugins.ANDROID_APPLICATION) ||
        pluginManager.hasPlugin(Plugins.ANDROID_LIBRARY)

internal val Project.androidExtensions: CommonExtension<*, *, *, *, *, *>
    get() {
        return if (pluginManager.hasPlugin(Plugins.ANDROID_APPLICATION)) {
            extensions.getByType<BaseAppModuleExtension>()
        } else if (pluginManager.hasPlugin(Plugins.ANDROID_LIBRARY)) {
            extensions.getByType<LibraryExtension>()
        } else {
            throw GradleException("The provided project does not have the Android plugin applied. ($name)")
        }
    }
