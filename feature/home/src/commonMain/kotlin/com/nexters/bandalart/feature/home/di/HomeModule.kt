package com.nexters.bandalart.feature.home.di

import com.nexters.bandalart.feature.home.viewmodel.HomeViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val homeModule = module {
    viewModelOf(::HomeViewModel)
}
