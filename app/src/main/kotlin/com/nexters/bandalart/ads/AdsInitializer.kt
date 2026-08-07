package com.nexters.bandalart.ads

import android.content.Context
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.nexters.bandalart.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

@Singleton
class AdsInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val isInitializationStarted = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun initialize() {
        if (!isInitializationStarted.compareAndSet(false, true)) return

        scope.launch {
            runCatching {
                MobileAds.initialize(
                    context,
                    InitializationConfig.Builder(context.getString(R.string.admob_app_id)).build(),
                ) {
                    Timber.d("GMA Next-Gen SDK initialized")
                }
            }.onFailure { exception ->
                isInitializationStarted.set(false)
                Timber.e(exception, "GMA Next-Gen SDK initialization failed")
            }
        }
    }
}
