plugins {
    alias(libs.plugins.bandalart.jvm.kotlin)
    alias(libs.plugins.bandalart.kotest)
}

dependencies {
    compileOnly(
        libs.compose.stable.marker,
    )
    implementations(
        libs.javax.inject,
        libs.kotlinx.coroutines.core,
    )
}
