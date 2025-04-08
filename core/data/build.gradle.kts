plugins {
    id("bandalart.lint")
    id("bandalart.kmp")
    id("bandalart.kmp.android")
    id("bandalart.kmp.ios")
    id("bandalart.room")
}

android.namespace = "com.nexters.bandalart.core.data"

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.database)
            implementation(projects.core.datastore)
            implementation(projects.core.domain)

            implementation(libs.koin.core)
        }

        androidUnitTest.dependencies {
            implementation(libs.bundles.android.unit.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
