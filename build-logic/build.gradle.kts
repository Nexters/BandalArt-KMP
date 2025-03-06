@file:Suppress("DSL_SCOPE_VIOLATION", "INLINE_FROM_HIGHER_PLATFORM")

plugins {
    `kotlin-dsl`
}

gradlePlugin {
    val conventionPluginClasses = listOf(
        "android.application" to "AndroidApplicationPlugin",
        "android.application.compose" to "AndroidApplicationComposePlugin",
        // "android.firebase" to "AndroidFirebasePlugin",
        "android.library" to "AndroidLibraryPlugin",
        "android.library.compose" to "AndroidLibraryComposePlugin",
        "android.feature" to "AndroidFeaturePlugin",
        // "android.room" to "AndroidRoomPlugin",
        "jvm.kotlin" to "JvmKotlinPlugin",
        "kotest" to "KotestPlugin",
        "kmp" to "KmpPlugin",
        "kmp.compose" to "KmpComposePlugin",
        "kmp.android" to "KmpAndroidPlugin",
        "kmp.ios" to "KmpIosPlugin",
        "kmp.firebase" to "KmpFirebasePlugin",
        "kotlin.serialization" to "KotlinSerializationPlugin",
        "room" to "RoomPlugin",
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
