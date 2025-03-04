package com.nexters.bandalart.buildlogic.primitive

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
