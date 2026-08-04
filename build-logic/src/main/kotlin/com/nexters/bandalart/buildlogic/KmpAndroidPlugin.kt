package com.nexters.bandalart.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.nexters.bandalart.buildlogic.configure.Plugins
import com.nexters.bandalart.buildlogic.configure.applyPlugins
import com.nexters.bandalart.buildlogic.configure.kotlin
import com.nexters.bandalart.buildlogic.configure.libs
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

internal class KmpAndroidPlugin : BuildLogicPlugin(
    {
        applyPlugins(Plugins.ANDROID_KMP_LIBRARY)
        val junitBom = dependencies.platform(libs.junit.bom)

        kotlin {
            targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
                namespace = if (path == ":composeApp") {
                    "com.nexters.bandalart.shared"
                } else {
                    "com.nexters.bandalart${path.replace(':', '.')}"
                }
                compileSdk = libs.versions.compileSdk.get().toInt()
                minSdk = libs.versions.minSdk.get().toInt()

                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                }

                if (file("src/androidHostTest").exists()) {
                    withHostTest {
                        isIncludeAndroidResources = true
                    }
                }
            }

            sourceSets.matching { it.name == "androidHostTest" }.configureEach {
                dependencies {
                    implementation(junitBom)
                    runtimeOnly(libs.junit.jupiter.engine)
                    runtimeOnly(libs.junit.platform.launcher)
                }
            }
        }
    },
)
