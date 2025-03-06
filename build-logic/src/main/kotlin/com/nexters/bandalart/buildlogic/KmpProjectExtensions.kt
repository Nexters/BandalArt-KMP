package com.nexters.bandalart.buildlogic

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

internal fun Project.configureKmp() {
    kotlinMultiPlatform {
        androidTarget {
            compilations.all {
                compileTaskProvider.configure {
                    compilerOptions {
                        jvmTarget.set(JvmTarget.JVM_17)
                    }
                }
            }
        }
    }
}
