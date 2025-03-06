package com.nexters.bandalart.buildlogic.configure

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestedExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

fun Project.androidApplication(action: BaseAppModuleExtension.() -> Unit) {
    extensions.configure(action)
}

fun Project.androidLibrary(action: LibraryExtension.() -> Unit) {
    extensions.configure(action)
}

fun Project.android(action: TestedExtension.() -> Unit) {
    extensions.configure(action)
}

fun Project.kotlinAndroidOptions(configure: KotlinAndroidProjectExtension.() -> Unit) {
    extensions.configure(configure)
}

fun Project.libraryAndroidOptions(configure: LibraryAndroidComponentsExtension.() -> Unit) {
    extensions.configure(configure)
}

fun Project.configureAndroid() {
    android {
        namespace?.let {
            this.namespace = it
        }
        compileSdkVersion(libs.versions.compileSdk.get().toInt())

        defaultConfig {
            minSdk = libs.versions.minSdk.get().toInt()
            targetSdk = libs.versions.targetSdk.get().toInt()
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
            isCoreLibraryDesugaringEnabled = true
        }
        dependencies {
            add("coreLibraryDesugaring", libs.desugar.jdk.libs)
        }
        testOptions {
            unitTests {
                isIncludeAndroidResources = true
            }
        }

        (this as CommonExtension<*, *, *, *, *, *>).lint {
            // shell friendly
            val filename = displayName.replace(":", "_").replace("[\\s']".toRegex(), "")

            xmlReport = true
            xmlOutput = rootProject.layout.buildDirectory.file("lint-reports/lint-results-$filename.xml").get().asFile
            htmlReport = true
            htmlOutput = rootProject.layout.buildDirectory.file("lint-reports/lint-results-$filename.html").get().asFile
            // for now
            sarifReport = false
        }
    }
}
