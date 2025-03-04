package com.netxters.bandalart.convention

import com.nexters.bandalart.convention.extension.kotlin
import com.nexters.bandalart.convention.extension.kspKmp
import com.nexters.bandalart.convention.extension.library
import com.nexters.bandalart.convention.extension.libs
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11

internal fun Project.configureKmp() {
    kotlinMultiPlatform {
        androidTarget {
            compilations.all {
                compileTaskProvider.configure {
                    compilerOptions {
                        jvmTarget.set(JVM_11)
                    }
                }
            }
        }
    }
}
