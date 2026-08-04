@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.bandalart.android.library)
    alias(libs.plugins.bandalart.android.library.compose)
}

android {
    namespace = "com.nexters.bandalart.core.common"

    defaultConfig {
        val major = libs.versions.majorVersion.get().toInt()
        val minor = libs.versions.minorVersion.get().toInt()
        val patch = libs.versions.patchVersion.get().toInt()
        val versionCode = (major * 10000) + (minor * 100) + patch

        buildConfigField("int", "VERSION_CODE", "$versionCode")
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementations(
        libs.kotlinx.datetime,

        libs.androidx.core,

        libs.app.update,
        libs.app.update.ktx,

        libs.timber,
        libs.bundles.retrofit,
    )
}
