package com.netxters.bandalart.convention

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.kotlin.dsl.NamedDomainObjectContainerScope
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.the

internal val Project.libs
    get() = the<LibrariesForLibs>()

internal fun Project.applyPlugins(vararg plugins: String) {
    plugins.forEach(pluginManager::apply)
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

internal inline operator fun <T : Any, C : NamedDomainObjectContainer<T>> C.invoke(
    configuration: Action<NamedDomainObjectContainerScope<T>>,
): C =
    apply {
        configuration.execute(NamedDomainObjectContainerScope.of(this))
    }

