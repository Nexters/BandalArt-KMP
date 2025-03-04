package com.netxters.bandalart.buildlogic.primitive

import org.gradle.api.Project
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestResult
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.KotlinClosure2
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

internal class KotestConventionPlugin : BuildLogicPlugin({
    useTestPlatformForTarget()
    dependencies {
        testImplementation(libs.test.kotest.framework)
    }
})

// ref: https://kotest.io/docs/quickstart#test-framework
private fun Project.useTestPlatformForTarget() {
    fun AbstractTestTask.setTestConfiguration() {
        // https://stackoverflow.com/a/36178581/14299073
        outputs.upToDateWhen { false }
        testLogging {
            events = setOf(
                TestLogEvent.PASSED,
                TestLogEvent.SKIPPED,
                TestLogEvent.FAILED,
            )
        }
        afterSuite(
            KotlinClosure2<TestDescriptor, TestResult, Unit>({ desc, result ->
                if (desc.parent == null) { // will match the outermost suite
                    val output = "Results: ${result.resultType} " +
                        "(${result.testCount} tests, " +
                        "${result.successfulTestCount} passed, " +
                        "${result.failedTestCount} failed, " +
                        "${result.skippedTestCount} skipped)"
                    println(output)
                }
            })
        )
    }

    if (isAndroidProject) {
        androidExtensions.testOptions {
            unitTests.all { test ->
                test.useJUnitPlatform()

                if (!test.name.contains("debug", ignoreCase = true)) {
                    test.enabled = false
                }
            }
        }
        tasks.withType<Test>().configureEach {
            setTestConfiguration()
        }
    } else {
        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
            setTestConfiguration()
        }
    }
}

