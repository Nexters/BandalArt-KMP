package com.nexters.bandalart.core.datastore.di

import com.nexters.bandalart.core.datastore.BandalartDataStore
import com.nexters.bandalart.core.datastore.BandalartDataStoreFactory
import com.nexters.bandalart.core.datastore.InAppUpdateDataStore
import org.koin.dsl.module

val dataStoreModule = module {
    single { get<BandalartDataStoreFactory>().createBandalartDataStore() }
    single { get<BandalartDataStoreFactory>().createInAppUpdateDataStore() }
    single { BandalartDataStore(get()) }
    single { InAppUpdateDataStore(get()) }
}
