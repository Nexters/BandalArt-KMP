import com.android.build.api.dsl.LibraryExtension
import com.netxters.bandalart.android.convention.Plugins
import com.netxters.bandalart.android.convention.applyPlugins
import com.netxters.bandalart.android.convention.configureCompose
import org.gradle.kotlin.dsl.configure

internal class AndroidLibraryComposeConventionPlugin : BuildLogicConventionPlugin(
    {
        applyPlugins(Plugins.ANDROID_LIBRARY, Plugins.KOTLIN_COMPOSE)

        extensions.configure<LibraryExtension> {
            configureCompose(this)
        }
    },
)

