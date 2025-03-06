package com.nexters.bandalart.feature.splash.di

import com.nexters.bandalart.feature.splash.SplashViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val splashModule = module {
    viewModelOf(::SplashViewModel)
}
