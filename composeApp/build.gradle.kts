import com.nexters.bandalart.buildlogic.task.GenerateBackupBuildConfigTask
import java.util.Properties
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    id("bandalart.lint")
    id("bandalart.kmp")
    id("bandalart.kmp.android")
    id("bandalart.kmp.compose")
    id("bandalart.kmp.ios")
    id("bandalart.kmp.firebase")
    alias(libs.plugins.metro)
}

val localProperties =
    Properties().apply {
        rootProject
            .file("local.properties")
            .takeIf { it.exists() }
            ?.inputStream()
            ?.use(::load)
    }
val backupSupabaseUrl =
    providers.gradleProperty("bandalart.supabaseUrl").orNull
        ?: providers.environmentVariable("BANDALART_SUPABASE_URL").orNull
        ?: localProperties.getProperty("bandalart.supabaseUrl").orEmpty()
val backupSupabasePublishableKey =
    providers.gradleProperty("bandalart.supabasePublishableKey").orNull
        ?: providers.environmentVariable("BANDALART_SUPABASE_PUBLISHABLE_KEY").orNull
        ?: localProperties.getProperty("bandalart.supabasePublishableKey").orEmpty()
val generatedBackupConfigDirectory = layout.buildDirectory.dir("generated/backupConfig/commonMain")
val generateBackupBuildConfig by tasks.registering(GenerateBackupBuildConfigTask::class) {
    supabaseUrl.set(backupSupabaseUrl)
    supabasePublishableKey.set(backupSupabasePublishableKey)
    outputFile.set(
        generatedBackupConfigDirectory.map {
            it.file("com/nexters/bandalart/backup/BackupBuildConfig.kt")
        },
    )
}

metro {
    enableCircuitCodegen.set(true)
}

kotlin {
    val xcfName = "ComposeApp"

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = xcfName
            isStatic = true
            binaryOption("smallBinary", "true")
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.work.runtime.ktx)
            implementation(libs.androidx.core)
        }

        androidHostTest.dependencies {
            implementation(libs.bundles.android.unit.test)
            implementation(libs.circuit.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.robolectric)
            implementation(libs.robolectric.junit5.extension)
            implementation(libs.androidx.work.testing)
        }

        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.data)
            implementation(projects.core.database)
            implementation(projects.core.datastore)
            implementation(projects.core.designsystem)
            implementation(projects.core.domain)
            implementation(projects.core.navigation)
            implementation(projects.core.ui)

            implementation(projects.feature.complete)
            implementation(projects.feature.backup)
            implementation(projects.feature.home)
            implementation(projects.feature.onboarding)
            implementation(projects.feature.splash)

            implementation(libs.androidx.datastore)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)

            implementation(libs.circuit.foundation)
            implementation(libs.circuit.runtime.presenter)
            implementation(libs.circuit.runtime.ui)

            implementation(libs.cmptoast)
            implementation(libs.jindong.compose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.napier)
        }
        getByName("commonMain").kotlin.srcDir(generatedBackupConfigDirectory)
    }

    compilerOptions.freeCompilerArgs.add("-Xexpect-actual-classes")
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    dependsOn(generateBackupBuildConfig)
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("-Djunit.platform.launcher.interceptors.enabled=true")
}
