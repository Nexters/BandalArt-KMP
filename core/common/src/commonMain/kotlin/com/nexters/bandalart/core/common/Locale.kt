package com.nexters.bandalart.core.common

interface Locale {
    val language: Language
}

enum class Language {
    KOREAN,
    ENGLISH,
    JAPANESE
}

expect fun getLocale(): Locale
