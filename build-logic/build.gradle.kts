plugins {
    `kotlin-dsl`
    alias(libs.plugins.gradle.dependency.handler.extensions)
}

gradlePlugin {
    val conventionPluginClasses = listOf(
        "android.application" to "AndroidApplicationConventionPlugin",
        "android.application.compose" to "AndroidApplicationComposeConventionPlugin",
        "android.firebase" to "AndroidFirebaseConventionPlugin",
        "android.library" to "AndroidLibraryConventionPlugin",
        "android.library.compose" to "AndroidLibraryComposeConventionPlugin",
        "android.feature" to "AndroidFeatureConventionPlugin",
        "android.hilt" to "AndroidHiltConventionPlugin",
        "android.retrofit" to "AndroidRetrofitConventionPlugin",
        "android.room" to "AndroidRoomConventionPlugin",
        "jvm.kotlin" to "JvmKotlinConventionPlugin",
        "kotest" to "KotestConventionPlugin",
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
    compileOnly(libs.gradle.android)
    compileOnly(libs.gradle.kotlin)
    compileOnly(libs.gradle.androidx.room)
    compileOnly(libs.compose.compiler.extension)

    compileOnly(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}

// Pair<PluginName, ClassName>
fun NamedDomainObjectContainer<PluginDeclaration>.pluginRegister(data: Pair<String, String>) {
    val (pluginName, className) = data
    register(pluginName) {
        id = "bandalart.$pluginName"
        implementationClass = className
    }
}
