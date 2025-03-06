package com.nexters.bandalart.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

expect class BandalartDataStoreFactory {
    fun createBandalartDataStore(): DataStore<Preferences>
    fun createInAppUpdateDataStore(): DataStore<Preferences>
}

internal const val BANDALART_DATA_STORE_FILE_NAME = "bandalart.preferences_pb"
internal const val IN_APP_UPDATE_DATA_STORE_FILE_NAME = "in_app_update.preferences_pb"
