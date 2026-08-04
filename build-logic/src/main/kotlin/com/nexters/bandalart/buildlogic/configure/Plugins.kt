package com.nexters.bandalart.buildlogic.configure

internal object Plugins {
    const val JAVA_LIBRARY = "java-library"

    const val KOTLIN_JVM = "org.jetbrains.kotlin.jvm"
    const val KOTLINX_SERIALIZATION = "org.jetbrains.kotlin.plugin.serialization"
    const val KOTLIN_COMPOSE = "org.jetbrains.kotlin.plugin.compose"

    const val ANDROID_APPLICATION = "com.android.application"
    const val ANDROID_LIBRARY = "com.android.library"
    const val ANDROID_KMP_LIBRARY = "com.android.kotlin.multiplatform.library"

    const val ANDROIDX_ROOM = "androidx.room"
    const val KSP = "com.google.devtools.ksp"

    const val GOOGLE_SERVICES = "com.google.gms.google-services"
    const val FIREBASE_CRASHLYTICS = "com.google.firebase.crashlytics"
}
