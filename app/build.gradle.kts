@file:Suppress("UnstableApiUsage")

import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import java.util.Properties

plugins {
    alias(libs.plugins.bandalart.android.application)
    alias(libs.plugins.bandalart.android.application.compose)
    alias(libs.plugins.bandalart.android.firebase)
    alias(libs.plugins.bandalart.android.hilt)
    alias(libs.plugins.google.secrets)
    alias(libs.plugins.play.publisher)
}

android {
    namespace = "com.nexters.bandalart"

    signingConfigs {
        create("release") {
            val propertiesFile = rootProject.file("keystore.properties")
            val properties = Properties()
            properties.load(propertiesFile.inputStream())
            storeFile = file(properties["STORE_FILE"] as String)
            storePassword = properties["STORE_PASSWORD"] as String
            keyAlias = properties["KEY_ALIAS"] as String
            keyPassword = properties["KEY_PASSWORD"] as String
        }
    }

    buildTypes {
        getByName("debug") {
            isDebuggable = true
            applicationIdSuffix = ".dev"
            manifestPlaceholders += mapOf(
                "appName" to "@string/app_name_dev",
            )
        }

        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            manifestPlaceholders += mapOf(
                "appName" to "@string/app_name",
            )
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

play {
    val serviceAccountFile = rootProject.file("playstore/service-account-key.json")
    if (serviceAccountFile.exists()) {
        serviceAccountCredentials.set(serviceAccountFile)
    }
    track.set("internal")
    releaseStatus.set(ReleaseStatus.COMPLETED)
    defaultToAppBundles.set(true)
}

dependencies {
    implementations(
        projects.core.data,
        projects.core.datastore,
        projects.core.designsystem,
        projects.core.domain,
        projects.core.network,
        projects.core.ui,
        projects.core.common,

        projects.feature.complete,
        projects.feature.home,
        projects.feature.onboarding,
        projects.feature.splash,

        libs.androidx.activity.compose,
        libs.androidx.splash,
        libs.androidx.startup,
        libs.androidx.core,
        libs.androidx.navigation.compose,
        libs.androidx.hilt.navigation.compose,
        libs.timber,

        libs.bundles.circuit
    )
    api(libs.circuit.codegen.annotation)
    ksp(libs.circuit.codegen.ksp)
}
