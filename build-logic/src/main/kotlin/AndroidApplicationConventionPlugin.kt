import com.android.build.api.dsl.ApplicationExtension
import com.netxters.bandalart.android.convention.Plugins
import com.netxters.bandalart.android.convention.applyPlugins
import com.netxters.bandalart.android.convention.configureAndroid
import com.netxters.bandalart.android.convention.libs
import org.gradle.kotlin.dsl.configure

internal class AndroidApplicationConventionPlugin : BuildLogicConventionPlugin(
    {
        applyPlugins(Plugins.ANDROID_APPLICATION)

        extensions.configure<ApplicationExtension> {
            configureAndroid(this)

            defaultConfig {
                applicationId = libs.versions.packageName.get()
                targetSdk = libs.versions.targetSdk.get().toInt()

                val major = libs.versions.majorVersion.get().toInt()
                val minor = libs.versions.minorVersion.get().toInt()
                val patch = libs.versions.patchVersion.get().toInt()

                versionCode = (major * 10000) + (minor * 100) + patch
                versionName = "$major.$minor.$patch"
            }
        }
    },
)
