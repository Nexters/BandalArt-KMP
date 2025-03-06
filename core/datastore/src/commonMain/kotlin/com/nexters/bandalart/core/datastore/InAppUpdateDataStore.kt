package com.nexters.bandalart.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first

class InAppUpdateDataStore(
    private val dataStore: DataStore<Preferences>,
) {

    private companion object {
        private const val REJECTED_VERSION_CODE = "rejected_version_code"
    }

    private val rejectedVersionCodeKey = intPreferencesKey(REJECTED_VERSION_CODE)

    suspend fun setLastRejectedUpdateVersion(rejectedVersionCode: Int) {
        dataStore.edit { preferences ->
            preferences[rejectedVersionCodeKey] = rejectedVersionCode
        }
    }

    suspend fun getLastRejectedUpdateVersion() = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }.first()[rejectedVersionCodeKey] ?: 0
}
