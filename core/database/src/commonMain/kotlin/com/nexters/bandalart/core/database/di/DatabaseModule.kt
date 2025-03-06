package com.nexters.bandalart.core.database.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.nexters.bandalart.core.database.BandalartDatabase
import com.nexters.bandalart.core.database.BandalartDatabaseFactory
import org.koin.dsl.module

val databaseModule = module {
    single {
        get<BandalartDatabaseFactory>().create()
            .setDriver(BundledSQLiteDriver())
            .build()
    }
    single { get<BandalartDatabase>().bandalartDao }
}
