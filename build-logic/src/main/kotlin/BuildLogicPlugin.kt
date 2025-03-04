import org.gradle.api.Plugin
import org.gradle.api.Project

abstract class BuildLogicPlugin(private val block: Project.() -> Unit) : Plugin<Project> {
    final override fun apply(target: Project) {
        with(target, block = block)
    }
}
