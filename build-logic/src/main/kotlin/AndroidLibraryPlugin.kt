import com.android.build.gradle.LibraryExtension
import com.netxters.bandalart.convention.Plugins
import com.netxters.bandalart.convention.applyPlugins
import com.netxters.bandalart.convention.configureAndroid
import com.netxters.bandalart.convention.libs
import org.gradle.kotlin.dsl.configure

internal class AndroidLibraryPlugin : BuildLogicPlugin({
    applyPlugins(Plugins.ANDROID_LIBRARY, Plugins.KOTLIN_ANDROID)

    extensions.configure<LibraryExtension> {
        configureAndroid(this)

        defaultConfig.apply {
            targetSdk = libs.versions.targetSdk.get().toInt()
        }
    }
})
