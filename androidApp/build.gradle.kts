import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import java.util.Properties

plugins {
    id("bandalart.lint")
    id("bandalart.android.application")
    id("bandalart.android.application.compose")
    id("bandalart.kotest")
    alias(libs.plugins.google.service)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.play.publisher)
}

val localProperties =
    Properties().apply {
        rootProject
            .file("local.properties")
            .takeIf { it.exists() }
            ?.inputStream()
            ?.use(::load)
    }
val supabaseUrl =
    providers.gradleProperty("bandalart.supabaseUrl").orNull
        ?: providers.environmentVariable("BANDALART_SUPABASE_URL").orNull
        ?: localProperties.getProperty("bandalart.supabaseUrl").orEmpty()
val supabasePublishableKey =
    providers.gradleProperty("bandalart.supabasePublishableKey").orNull
        ?: providers.environmentVariable("BANDALART_SUPABASE_PUBLISHABLE_KEY").orNull
        ?: localProperties.getProperty("bandalart.supabasePublishableKey").orEmpty()

fun String.asBuildConfigString(): String = "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

android {
    namespace = "com.nexters.bandalart"

    defaultConfig {
        buildConfigField("String", "SUPABASE_URL", supabaseUrl.asBuildConfigString())
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", supabasePublishableKey.asBuildConfigString())
    }

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
            resValue("string", "admob_app_id", "ca-app-pub-3940256099942544~3347511713")
            resValue("string", "admob_rewarded_ad_unit_id", "ca-app-pub-3940256099942544/5224354917")
            resValue("string", "admob_banner_ad_unit_id", "ca-app-pub-3940256099942544/6300978111")
            manifestPlaceholders +=
                mapOf(
                    "appName" to "@string/app_name_dev",
                )
        }

        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            resValue("string", "admob_app_id", "ca-app-pub-5570932833347277~6079637815")
            resValue("string", "admob_rewarded_ad_unit_id", "ca-app-pub-5570932833347277/6659503579")
            resValue("string", "admob_banner_ad_unit_id", "ca-app-pub-5570932833347277/1215605203")
            manifestPlaceholders +=
                mapOf(
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
        resValues = true
    }
}

play {
    val serviceAccountFile = rootProject.file("playstore/service-account-key.json")
    if (serviceAccountFile.exists()) {
        serviceAccountCredentials.set(serviceAccountFile)
    }
    track.set("internal")
    releaseStatus.set(ReleaseStatus.COMPLETED)
    updatePriority.set(0)
    defaultToAppBundles.set(true)
}

dependencies {
    implementation(projects.composeApp)
    implementation(projects.core.common)
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.core.domain)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.splash)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.app.update)

    implementation(libs.firebase.common)
    implementation(libs.google.mobile.ads.next.gen)

    implementation(libs.cmptoast)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.napier)
    implementation(platform(libs.firebase.bom))
    debugImplementation(libs.ding) {
        exclude(group = "com.google.firebase", module = "firebase-bom")
    }
    "releaseImplementation"(libs.ding.noop) {
        exclude(group = "com.google.firebase", module = "firebase-bom")
    }
    testImplementation(libs.bundles.android.unit.test)
    testRuntimeOnly(libs.junit.jupiter.engine)
    "baselineProfile"(project(":baselineprofile"))
}
