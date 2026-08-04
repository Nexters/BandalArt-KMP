plugins {
    id("bandalart.lint")
    id("bandalart.kmp")
    id("bandalart.kmp.android")
    id("bandalart.kmp.ios")
    id("bandalart.kmp.compose")
}

kotlin {
    compilerOptions.freeCompilerArgs.add("-Xexpect-actual-classes")
}

compose.resources {
    publicResClass = true
}
