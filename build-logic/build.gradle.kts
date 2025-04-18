@file:Suppress("DSL_SCOPE_VIOLATION", "INLINE_FROM_HIGHER_PLATFORM")

plugins {
    `kotlin-dsl`
}

gradlePlugin {
    val conventionPluginClasses = listOf(
        "android.application" to "AndroidApplicationPlugin",
        "android.application.compose" to "AndroidApplicationComposePlugin",
        "android.library" to "AndroidLibraryPlugin",
        "android.library.compose" to "AndroidLibraryComposePlugin",
        "jvm.kotlin" to "JvmKotlinPlugin",
        "kotest" to "KotestPlugin",
        "kmp" to "KmpPlugin",
        "kmp.compose" to "KmpComposePlugin",
        "kmp.android" to "KmpAndroidPlugin",
        "kmp.ios" to "KmpIosPlugin",
        "kmp.firebase" to "KmpFirebasePlugin",
        "kmp.feature" to "KmpFeaturePlugin",
        "kotlin.serialization" to "KotlinSerializationPlugin",
        "room" to "RoomPlugin",
        "detekt" to "DetektPlugin",
        "spotless" to "SpotlessPlugin",
        "lint" to "LintPlugin",
    )

    plugins {
        conventionPluginClasses.forEach { pluginClass ->
            pluginRegister(pluginClass)
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.bundles.plugins)

    compileOnly(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}

// Pair<PluginName, ClassName>
fun NamedDomainObjectContainer<PluginDeclaration>.pluginRegister(data: Pair<String, String>) {
    val (pluginName, className) = data
    register(pluginName) {
        id = "bandalart.$pluginName"
        implementationClass = "com.nexters.bandalart.buildlogic.${className}"
    }
}
