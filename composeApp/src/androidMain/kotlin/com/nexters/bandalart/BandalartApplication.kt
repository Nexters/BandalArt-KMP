package com.nexters.bandalart

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.initialize
import com.nexters.bandalart.di.initKoin
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.android.ext.koin.androidContext

class BandalartApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Napier.base(DebugAntilog())
        }

        initKoin {
            androidContext(this@BandalartApplication)
        }

        Firebase.initialize(this)

        multiplatform.network.cmptoast.AppContext.apply { set(applicationContext) }
    }
}
