package com.nexters.bandalart.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

actual class BandalartDataStoreFactory(private val context: Context) {
    actual fun createBandalartDataStore(): DataStore<Preferences> {
        return PreferenceDataStoreFactory.createWithPath(
            produceFile = {
                context.filesDir.resolve(BANDALART_DATA_STORE_FILE_NAME).absolutePath.toPath()
            }
        )
    }

    actual fun createInAppUpdateDataStore(): DataStore<Preferences> {
        return PreferenceDataStoreFactory.createWithPath(
            produceFile = {
                context.filesDir.resolve(IN_APP_UPDATE_DATA_STORE_FILE_NAME).absolutePath.toPath()
            }
        )
    }
}
