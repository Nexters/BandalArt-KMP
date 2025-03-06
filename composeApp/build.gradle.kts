import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    id("bandalart.kmp")
    id("bandalart.kmp.compose")
    id("bandalart.kmp.ios")
    id("bandalart.android.application")
    id("bandalart.android.feature")
    // id("bandalart.room")
    // alias(libs.plugins.buildkonfig)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    val xcfName = "ComposeApp"

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = xcfName
            isStatic = true
        }
    }

    sourceSets {
        androidMain {
            dependencies {
                implementation(libs.androidx.compose.ui.tooling.preview)
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.splash)

                implementation(libs.koin.android)
                // implementation(libs.koin.androidx.startup)
            }
        }

        commonMain {
            dependencies {
                implementation(projects.core.common)
                implementation(projects.core.data)
                implementation(projects.core.database)
                implementation(projects.core.datastore)
                implementation(projects.core.designsystem)
                implementation(projects.core.domain)
                implementation(projects.core.navigation)
                implementation(projects.core.ui)

                implementation(projects.feature.complete)
                implementation(projects.feature.home)
                implementation(projects.feature.onboarding)
                implementation(projects.feature.splash)

                implementation(libs.kotlinx.collections.immutable)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.json)

                implementation(libs.androidx.lifecycle.viewmodel)
                implementation(libs.navigation.compose)

                implementation(libs.androidx.room.runtime)
                implementation(libs.androidx.sqlite.bundled)

                api(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)

                implementation(libs.napier)
                implementation(libs.uri.kmp)
                implementation(libs.compottie)
                implementation(libs.filekit.core)
                implementation(libs.cmptoast)

                implementation(libs.coil3.compose)
                implementation(libs.landscapist.coil3)
                implementation(libs.landscapist.placeholder)

                implementation(libs.filekit.core)
                implementation(libs.filekit.compose)
            }
        }

        iosMain {
            dependencies {}
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

android {
    namespace = "com.nexters.bandalart"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()

        versionCode = 1
        versionName = "1.0"
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }
}

//dependencies {
//    add("kspAndroid", libs.androidx.room.compiler)
//    add("kspIosX64", libs.androidx.room.compiler)
//    add("kspIosArm64", libs.androidx.room.compiler)
//    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
//}
//
//room {
//    schemaDirectory("$projectDir/schemas")
//}
