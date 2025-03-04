import com.netxters.bandalart.convention.kotlin
import com.netxters.bandalart.convention.libs
import com.netxters.bandalart.convention.kspKmp
import org.gradle.api.Plugin
import org.gradle.api.Project

class RoomPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.google.devtools.ksp")
            }

            kotlin {
                with(sourceSets) {
                    getByName("commonMain").apply {
                        dependencies {
                            implementation(libs.androidx.room.runtime)
                            implementation(libs.androidx.sqlite.bundled)
                        }
                    }
                }
            }
            dependencies {
                kspKmp(libs.androidx.room.compiler)
            }
        }
    }
}
