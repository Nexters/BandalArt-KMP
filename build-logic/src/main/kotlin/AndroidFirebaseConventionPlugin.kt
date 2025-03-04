import com.netxters.bandalart.convention.Plugins
import com.netxters.bandalart.convention.applyPlugins
import com.netxters.bandalart.convention.implementation
import com.netxters.bandalart.convention.libs
import org.gradle.kotlin.dsl.dependencies

internal class AndroidFirebaseConventionPlugin : BuildLogicConventionPlugin(
    {
        applyPlugins(Plugins.GOOGLE_SERVICES, Plugins.FIREBASE_CRASHLYTICS)

        dependencies {
            implementation(platform(libs.firebase.bom))
            implementation(libs.firebase.analytics)
            implementation(libs.firebase.crashlytics)
        }
    },
)
