package com.nexters.bandalart.feature.complete.di

import com.nexters.bandalart.feature.complete.viewmodel.CompleteViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val completeModule = module {
    viewModelOf(::CompleteViewModel)
}
