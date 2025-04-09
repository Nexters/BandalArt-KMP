/*
 * Copyright 2025 easyhooon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nexters.bandalart.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

class BandalartDataStore(
    private val dataStore: DataStore<Preferences>,
) {
    private companion object {
        private const val RECENT_BANDALART_ID = "recent_bandalart_id"
        private const val COMPLETED_BANDALART_LIST_ID = "completed_bandalart_list_id"
        private const val ONBOARDING_COMPLETED_ID = "completed_onboarding_id"
    }

    private val recentBandalartKey = longPreferencesKey(RECENT_BANDALART_ID)
    private val completedBandalartListKey = stringPreferencesKey(COMPLETED_BANDALART_LIST_ID)
    private val onboardingCompletedKey = booleanPreferencesKey(ONBOARDING_COMPLETED_ID)

    suspend fun setRecentBandalartId(recentBandalartId: Long) {
        dataStore.edit { preferences ->
            preferences[recentBandalartKey] = recentBandalartId
        }
    }

    suspend fun getRecentBandalartId() =
        dataStore.data
            .catch { exception ->
                if (exception is IOException)
                    emit(emptyPreferences())
                else
                    throw exception
            }.first()[recentBandalartKey] ?: 0L

    suspend fun getPrevBandalartList() =
        stringToList(
            dataStore.data
                .catch { exception ->
                    if (exception is IOException)
                        emit(emptyPreferences())
                    else
                        throw exception
                }.first()[completedBandalartListKey] ?: "",
        )

    // 키가 존재하면 값을 갱신, 없으면 추가
    suspend fun upsertBandalartId(
        bandalartId: Long,
        isCompleted: Boolean
    ) {
        dataStore.edit { preferences ->
            val currentListAsString = preferences[completedBandalartListKey] ?: ""
            val currentList = stringToList(currentListAsString)
            val isKeyExists = currentList.any { it.first == bandalartId }
            val updatedList =
                if (isKeyExists) {
                    currentList.map {
                        if (it.first == bandalartId)
                            Pair(bandalartId, isCompleted)
                        else
                            it
                    }
                } else {
                    currentList + Pair(bandalartId, isCompleted)
                }
            preferences[completedBandalartListKey] = listToString(updatedList)
        }
    }

    // 목표를 달성하지 못했었는데 이번에 달성한 경우를 검사
    suspend fun checkCompletedBandalartId(bandalartId: Long): Boolean =
        dataStore.data
            .catch { exception ->
                if (exception is IOException)
                    emit(emptyPreferences())
                else
                    throw exception
            }.first()[completedBandalartListKey]
            ?.let { currentListAsString ->
                val currentList = stringToList(currentListAsString)
                // 이전에 목표를 달성하지 않았었는지 확인
                val wasCompleted = currentList.find { it.first == bandalartId }?.second ?: false
                !wasCompleted
            } ?: false

    suspend fun deleteBandalartId(bandalartId: Long) {
        dataStore.edit { preferences ->
            val currentListAsString = preferences[completedBandalartListKey] ?: ""
            val currentList = stringToList(currentListAsString)
            val updatedList = currentList.filter { it.first != bandalartId }
            preferences[completedBandalartListKey] = listToString(updatedList)
        }
    }

    private fun listToString(list: List<Pair<Long, Boolean>>): String = Json.encodeToString(list)

    private fun stringToList(data: String): List<Pair<Long, Boolean>> {
        if (data.isEmpty()) return emptyList()
        return Json.decodeFromString(data)
    }

    suspend fun setOnboardingCompletedStatus(flag: Boolean) {
        dataStore.edit { preferences ->
            preferences[onboardingCompletedKey] = flag
        }
    }

    suspend fun getOnboardingCompletedStatus() =
        dataStore.data
            .catch { exception ->
                if (exception is IOException)
                    emit(emptyPreferences())
                else
                    throw exception
            }.first()[onboardingCompletedKey] ?: false
}
