plugins {
    id("template.convention.kmp")
    id("template.convention.kmp.android")
    id("template.convention.kmp.ios")
}

android.namespace = "com.nexters.bandalart.core.ui"

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.datastore.core)
            api(projects.core.model)

            implementation(libs.kotlinx.datetime)
        }
    }
}
