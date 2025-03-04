import com.netxters.bandalart.convention.Plugins
import com.netxters.bandalart.convention.ApplicationConfig
import com.netxters.bandalart.convention.applyPlugins
import com.netxters.bandalart.convention.detektPlugins
import com.netxters.bandalart.convention.libs
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

internal class JvmKotlinConventionPlugin : BuildLogicConventionPlugin({
    applyPlugins(Plugins.JAVA_LIBRARY, Plugins.KOTLIN_JVM)

    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = ApplicationConfig.JavaVersion
        targetCompatibility = ApplicationConfig.JavaVersion
    }

    extensions.configure<KotlinProjectExtension> {
        jvmToolchain(ApplicationConfig.JavaVersionAsInt)
    }

    dependencies {
        detektPlugins(libs.detekt.formatting)
    }
})
