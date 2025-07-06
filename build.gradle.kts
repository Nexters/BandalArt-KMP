@file:Suppress("DSL_SCOPE_VIOLATION")

import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    alias(libs.plugins.gradle.dependency.handler.extensions)
    alias(libs.plugins.kotlin.detekt)
    alias(libs.plugins.kotlin.ktlint)
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.androidx.room) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.google.service) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.ksp) apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }

    apply {
        plugin(rootProject.libs.plugins.kotlin.detekt.get().pluginId)
        plugin(rootProject.libs.plugins.kotlin.ktlint.get().pluginId)
        plugin(rootProject.libs.plugins.gradle.dependency.handler.extensions.get().pluginId)
    }

    afterEvaluate {
        extensions.configure<DetektExtension> {
            parallel = true
            buildUponDefaultConfig = true
            toolVersion = libs.versions.kotlin.detekt.get()
            config.setFrom(files("$rootDir/detekt-config.yml"))
        }

        extensions.configure<KtlintExtension> {
            version.set(rootProject.libs.versions.kotlin.ktlint.source.get())
            android.set(true)
            verbose.set(true)
        }
    }
}

tasks.register("cleanAll", type = Delete::class) {
    allprojects.map(Project::getBuildDir).forEach(::delete)
}

tasks.register("clean", type = Delete::class) {
    rootProject.buildDir.delete()
}

tasks.register("bundleRelease", type = Exec::class) {
    commandLine(project.rootDir.resolve("gradlew"), "bundle")
    workingDir = project.rootDir
}

tasks.register("release") {
    dependsOn(tasks["clean"])
    dependsOn(tasks["bundleRelease"])
    mustRunAfter(tasks["clean"])
}

apply {
    from("gradle/projectDependencyGraph.gradle")
}
