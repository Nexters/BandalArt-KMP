import com.android.build.api.dsl.ApplicationExtension
import com.netxters.bandalart.convention.Plugins
import com.netxters.bandalart.convention.applyPlugins
import com.netxters.bandalart.convention.configureCompose
import org.gradle.kotlin.dsl.configure

internal class AndroidApplicationComposeConventionPlugin : BuildLogicConventionPlugin(
    {
        applyPlugins(Plugins.ANDROID_APPLICATION, Plugins.KOTLIN_COMPOSE)

        extensions.configure<ApplicationExtension> {
            configureCompose(this)
        }
    },
)
