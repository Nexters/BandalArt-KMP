import com.android.build.api.dsl.LibraryExtension
import com.netxters.bandalart.android.convention.Plugins
import com.netxters.bandalart.android.convention.applyPlugins
import com.netxters.bandalart.android.convention.configureAndroid
import org.gradle.kotlin.dsl.configure

internal class AndroidLibraryConventionPlugin : BuildLogicConventionPlugin({
    applyPlugins(Plugins.ANDROID_LIBRARY)

    extensions.configure<LibraryExtension> {
        configureAndroid(this)
    }
})
