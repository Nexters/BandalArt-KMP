plugins {
    id("template.convention.kmp")
    id("template.convention.kmp.android")
    id("template.convention.kmp.ios")
    id("template.convention.room")
}

android.namespace = "com.nexters.bandalart.core.ui"

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)

            implementation(libs.kotlinx.datetime)
        }
    }
}
