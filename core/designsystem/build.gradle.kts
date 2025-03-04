plugins {
    id("template.convention.kmp")
    id("template.convention.kmp.android")
    id("template.convention.kmp.ios")
    id("template.convention.kmp.compose")
}

android.namespace = "com.nexters.bandalart.core.ui"

compose.resources {
    publicResClass = true
}
