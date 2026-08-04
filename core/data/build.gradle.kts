@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.bandalart.android.library)
    alias(libs.plugins.bandalart.android.hilt)
    alias(libs.plugins.bandalart.android.retrofit)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.nexters.bandalart.core.data"

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementations(
        projects.core.domain,
        projects.core.datastore,
        projects.core.database,
        projects.core.network,

        libs.bundles.ktor.client,
        libs.timber,
    )
}
