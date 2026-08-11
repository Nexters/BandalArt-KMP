plugins {
    id("bandalart.lint")
    id("bandalart.kmp")
    id("bandalart.kmp.android")
    id("bandalart.kmp.ios")
    id("bandalart.room")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.database)
            implementation(projects.core.datastore)
            implementation(projects.core.domain)
        }

        androidHostTest.dependencies {
            implementation(libs.bundles.android.unit.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kotlinx.datetime)
            implementation(libs.robolectric)
            implementation(libs.robolectric.junit5.extension)
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("-Djunit.platform.launcher.interceptors.enabled=true")
}
