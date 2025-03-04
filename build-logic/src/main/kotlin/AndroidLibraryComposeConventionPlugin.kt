import com.android.build.gradle.LibraryExtension
import com.netxters.bandalart.convention.Plugins
import com.netxters.bandalart.convention.applyPlugins
import com.netxters.bandalart.convention.configureCompose
import org.gradle.kotlin.dsl.configure

internal class AndroidLibraryComposeConventionPlugin : BuildLogicConventionPlugin(
    {
        applyPlugins(Plugins.ANDROID_LIBRARY, Plugins.KOTLIN_COMPOSE)

        extensions.configure<LibraryExtension> {
            configureCompose(this)
        }
    },
)


