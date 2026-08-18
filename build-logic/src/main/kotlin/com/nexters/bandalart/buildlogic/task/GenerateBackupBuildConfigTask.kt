package com.nexters.bandalart.buildlogic.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class GenerateBackupBuildConfigTask : DefaultTask() {
    @get:Input
    abstract val supabaseUrl: Property<String>

    @get:Input
    abstract val supabasePublishableKey: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val destination = outputFile.get().asFile
        destination.parentFile.mkdirs()
        destination.writeText(
            """
            package com.nexters.bandalart.backup

            internal object BackupBuildConfig {
                const val SUPABASE_URL = ${supabaseUrl.get().asKotlinStringLiteral()}
                const val SUPABASE_PUBLISHABLE_KEY = ${supabasePublishableKey.get().asKotlinStringLiteral()}
            }
            """.trimIndent(),
        )
    }

    private fun String.asKotlinStringLiteral(): String = "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""
}
