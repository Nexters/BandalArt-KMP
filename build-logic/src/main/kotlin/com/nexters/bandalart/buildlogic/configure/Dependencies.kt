package com.nexters.bandalart.buildlogic.configure

import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler

internal fun DependencyHandler.implementation(dependencyNotation: Any): Dependency? {
    return add("implementation", dependencyNotation)
}

internal fun DependencyHandler.api(dependencyNotation: Any): Dependency? {
    return add("api", dependencyNotation)
}

internal fun DependencyHandler.ksp(dependencyNotation: Any): Dependency? {
    return add("ksp", dependencyNotation)
}

internal fun DependencyHandler.androidTestImplementation(dependencyNotation: Any): Dependency? {
    return add("androidTestImplementation", dependencyNotation)
}

internal fun DependencyHandler.debugImplementation(dependencyNotation: Any): Dependency? {
    return add("debugImplementation", dependencyNotation)
}

internal fun DependencyHandler.testImplementation(dependencyNotation: Any): Dependency? {
    return add("testImplementation", dependencyNotation)
}

internal fun DependencyHandler.coreLibraryDesugaring(dependencyNotation: Any): Dependency? {
    return add("coreLibraryDesugaring", dependencyNotation)
}

internal fun DependencyHandler.detektPlugins(dependencyNotation: Any): Dependency? {
    return add("detektPlugins", dependencyNotation)
}

internal fun DependencyHandler.compileOnly(dependencyNotation: Any): Dependency? {
    return add("compileOnly", dependencyNotation)
}

internal fun DependencyHandler.project(
    path: String,
    configuration: String? = null,
): Dependency {
    return project(
        mapOf(
            "path" to path,
            "configuration" to configuration,
        ).filterValues { it != null },
    )
}
