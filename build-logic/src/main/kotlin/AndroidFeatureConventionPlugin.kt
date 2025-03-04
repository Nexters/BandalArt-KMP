import com.netxters.bandalart.convention.Plugins.KOTLIN_PARCELIZE
import com.netxters.bandalart.convention.applyPlugins
import com.netxters.bandalart.convention.implementation
import com.netxters.bandalart.convention.libs
import com.netxters.bandalart.convention.project
import org.gradle.kotlin.dsl.dependencies

internal class AndroidFeatureConventionPlugin : BuildLogicConventionPlugin(
    {
        applyPlugins(
            "bandalart.android.library",
            "bandalart.android.library.compose",
            KOTLIN_PARCELIZE,
        )

        dependencies {
            implementation(project(path = ":core:common"))
            implementation(project(path = ":core:designsystem"))
            implementation(project(path = ":core:domain"))
            implementation(project(path = ":core:ui"))
            implementation(project(path = ":core:navigation"))

            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.viewmodel.navigation)
        }
    },
)
