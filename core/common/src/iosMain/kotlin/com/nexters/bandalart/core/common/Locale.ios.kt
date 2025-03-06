package com.nexters.bandalart.core.common

import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

// https://github.com/wisemuji/compose-would-you-rather-game/issues/2
class IOSLocale : Locale {
    override val language: Language
        get() = NSLocale
            .preferredLanguages()
            .first()
            .let {
                when (it) {
                    "ko-KR" -> Language.KOREAN
                    "ja-JP" -> Language.JAPANESE
                    else -> Language.ENGLISH
                }
            }
}

actual fun getLocale(): Locale = IOSLocale()
