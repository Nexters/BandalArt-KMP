@file:Suppress("INLINE_FROM_HIGHER_PLATFORM", "UnstableApiUsage")

plugins {
    alias(libs.plugins.bandalart.android.library)
    alias(libs.plugins.bandalart.android.hilt)
    alias(libs.plugins.bandalart.android.retrofit)
    alias(libs.plugins.google.secrets)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.nexters.bandalart.core.network"

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementations(
        projects.core.datastore,

        libs.bundles.ktor.client,
        libs.timber,
    )
}

secrets {
    propertiesFileName = "secrets.properties"
    defaultPropertiesFileName = "local.defaults.properties"
}
