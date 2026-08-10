plugins {
    id("bandalart.lint")
    id("bandalart.kmp")
    id("bandalart.kmp.ios")
}

kotlin {
    jvm()

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = "IosWidgetShared"
            isStatic = true
        }
    }

    sourceSets {
        iosMain.dependencies {
            implementation(projects.core.database)
            implementation(libs.kotlinx.coroutines.core)
        }

        jvmTest.dependencies {
            implementation(kotlin("test-junit5"))
            runtimeOnly(libs.junit.jupiter.engine)
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
