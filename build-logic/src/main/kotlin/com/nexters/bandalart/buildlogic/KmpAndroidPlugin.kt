package com.nexters.bandalart.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.nexters.bandalart.buildlogic.configure.Plugins
import com.nexters.bandalart.buildlogic.configure.applyPlugins
import com.nexters.bandalart.buildlogic.configure.kotlin
import com.nexters.bandalart.buildlogic.configure.libs
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.getByType
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
                compileSdk {
                    version = release(libs.versions.compileSdk.get().toInt()) {
                        minorApiLevel = libs.versions.compileSdkMinor.get().toInt()
                    }
                }
                minSdk = libs.versions.minSdk.get().toInt()

                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                }

                if (file("src/androidHostTest").exists()) {
                    withHostTest {
                        isIncludeAndroidResources = true
                        isReturnDefaultValues = true
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

        // 컴파일 대상은 Java 17로 두고, 호스트 테스트만 JDK 21에서 실행한다.
        // hig 전이 의존성(zone.ien.capsule)이 Java 21 바이트코드라 JDK 17 Robolectric에서 로드되지 않는다.
        val javaToolchains = extensions.getByType<JavaToolchainService>()
        tasks.withType<Test>().configureEach {
            javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) })
        }
    },
)
