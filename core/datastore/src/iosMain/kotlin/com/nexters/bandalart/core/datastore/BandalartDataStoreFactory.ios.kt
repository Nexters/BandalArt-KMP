package com.nexters.bandalart.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual class BandalartDataStoreFactory {
    actual fun createBandalartDataStore(): DataStore<Preferences> {
        return PreferenceDataStoreFactory.createWithPath(
            produceFile = {
                getDocumentPath(BANDALART_DATA_STORE_FILE_NAME).toPath()
            }
        )
    }

    actual fun createInAppUpdateDataStore(): DataStore<Preferences> {
        return PreferenceDataStoreFactory.createWithPath(
            produceFile = {
                getDocumentPath(IN_APP_UPDATE_DATA_STORE_FILE_NAME).toPath()
            }
        )
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun getDocumentPath(fileName: String): String {
        val directory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
        return requireNotNull(directory).path + "/$fileName"
    }
}
