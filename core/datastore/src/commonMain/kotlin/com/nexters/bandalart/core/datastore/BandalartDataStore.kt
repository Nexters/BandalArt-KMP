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
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class BandalartDataStore(
    private val dataStore: DataStore<Preferences>,
) {
    private companion object {
        private const val RECENT_BANDALART_ID = "recent_bandalart_id"
        private const val RECENT_SUB_GOAL_ID_PREFIX = "recent_sub_goal_id_"
        private const val COMPLETED_BANDALART_LIST_ID = "completed_bandalart_list_id"
        private const val ONBOARDING_COMPLETED_ID = "completed_onboarding_id"
        private const val THEME_MODE = "theme_mode"
        private const val RECENT_EMOJIS = "recent_emojis"
        private const val DEADLINE_REMINDER_ENABLED = "deadline_reminder_enabled"
        private const val MAX_BANDALART_SLOTS = "max_bandalart_slots"
        private const val PENDING_REWARDED_REQUEST_ID = "pending_rewarded_request_id"
        private const val PENDING_REWARDED_TARGET_SLOTS = "pending_rewarded_target_slots"
        private const val PENDING_REWARDED_GRANTED = "pending_rewarded_granted"
        private const val PENDING_REWARDED_TEMPLATE_ID = "pending_rewarded_template_id"
        private const val MAX_RECENT_EMOJIS = 12
    }

    private val recentBandalartKey = longPreferencesKey(RECENT_BANDALART_ID)
    private val completedBandalartListKey = stringPreferencesKey(COMPLETED_BANDALART_LIST_ID)
    private val onboardingCompletedKey = booleanPreferencesKey(ONBOARDING_COMPLETED_ID)
    private val themeModeKey = stringPreferencesKey(THEME_MODE)
    private val recentEmojisKey = stringPreferencesKey(RECENT_EMOJIS)
    private val deadlineReminderEnabledKey = booleanPreferencesKey(DEADLINE_REMINDER_ENABLED)
    private val maxBandalartSlotsKey = intPreferencesKey(MAX_BANDALART_SLOTS)
    private val pendingRewardedRequestIdKey = longPreferencesKey(PENDING_REWARDED_REQUEST_ID)
    private val pendingRewardedTargetSlotsKey = intPreferencesKey(PENDING_REWARDED_TARGET_SLOTS)
    private val pendingRewardedGrantedKey = booleanPreferencesKey(PENDING_REWARDED_GRANTED)
    private val pendingRewardedTemplateIdKey = stringPreferencesKey(PENDING_REWARDED_TEMPLATE_ID)

    suspend fun resolveMaxBandalartSlots(minimumSlots: Int): Int {
        var resolvedSlots = minimumSlots
        dataStore.edit { preferences ->
            resolvedSlots = maxOf(preferences[maxBandalartSlotsKey] ?: 0, minimumSlots)
            preferences[maxBandalartSlotsKey] = resolvedSlots
        }
        return resolvedSlots
    }

    suspend fun expandMaxBandalartSlots(minimumSlots: Int): Int {
        var expandedSlots = minimumSlots + 1
        dataStore.edit { preferences ->
            expandedSlots = maxOf(preferences[maxBandalartSlotsKey] ?: 0, minimumSlots) + 1
            preferences[maxBandalartSlotsKey] = expandedSlots
        }
        return expandedSlots
    }

    suspend fun prepareRewardedCreation(
        requestId: Long,
        minimumSlots: Int,
        templateId: String? = null,
    ): StoredPendingRewardedCreation {
        var pending = StoredPendingRewardedCreation(requestId, minimumSlots + 1, false, templateId)
        dataStore.edit { preferences ->
            val resolvedSlots = maxOf(preferences[maxBandalartSlotsKey] ?: 0, minimumSlots)
            pending = StoredPendingRewardedCreation(requestId, resolvedSlots + 1, false, templateId)
            preferences[pendingRewardedRequestIdKey] = pending.requestId
            preferences[pendingRewardedTargetSlotsKey] = pending.targetSlots
            preferences[pendingRewardedGrantedKey] = false
            if (templateId == null) {
                preferences.remove(pendingRewardedTemplateIdKey)
            } else {
                preferences[pendingRewardedTemplateIdKey] = templateId
            }
        }
        return pending
    }

    suspend fun grantRewardedCreation(requestId: Long): StoredPendingRewardedCreation? {
        var granted: StoredPendingRewardedCreation? = null
        dataStore.edit { preferences ->
            val pending = preferences.pendingRewardedCreation() ?: return@edit
            if (pending.requestId != requestId) return@edit

            granted = pending.copy(isGranted = true)
            preferences[pendingRewardedGrantedKey] = true
            preferences[maxBandalartSlotsKey] =
                maxOf(preferences[maxBandalartSlotsKey] ?: 0, pending.targetSlots)
        }
        return granted
    }

    suspend fun getPendingRewardedCreation(): StoredPendingRewardedCreation? =
        dataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            }.first()
            .pendingRewardedCreation()

    suspend fun clearPendingRewardedCreation(requestId: Long) {
        dataStore.edit { preferences ->
            if (preferences[pendingRewardedRequestIdKey] != requestId) return@edit
            preferences.remove(pendingRewardedRequestIdKey)
            preferences.remove(pendingRewardedTargetSlotsKey)
            preferences.remove(pendingRewardedGrantedKey)
            preferences.remove(pendingRewardedTemplateIdKey)
        }
    }

    val themeMode =
        dataStore.data
            .catch { exception ->
                if (exception is IOException)
                    emit(emptyPreferences())
                else
                    throw exception
            }.map { preferences -> preferences[themeModeKey] }

    val recentEmojis =
        dataStore.data
            .catch { exception ->
                if (exception is IOException)
                    emit(emptyPreferences())
                else
                    throw exception
            }.map { preferences ->
                preferences[recentEmojisKey]
                    ?.let { Json.decodeFromString<List<String>>(it) }
                    ?: emptyList()
            }

    val deadlineReminderEnabled =
        dataStore.data
            .catch { exception ->
                if (exception is IOException)
                    emit(emptyPreferences())
                else
                    throw exception
            }.map { preferences -> preferences[deadlineReminderEnabledKey] ?: false }

    val recentBandalartId =
        dataStore.data
            .catch { exception ->
                if (exception is IOException)
                    emit(emptyPreferences())
                else
                    throw exception
            }.map { preferences -> preferences[recentBandalartKey] ?: 0L }
            .distinctUntilChanged()

    val recentSubGoalId =
        dataStore.data
            .catch { exception ->
                if (exception is IOException)
                    emit(emptyPreferences())
                else
                    throw exception
            }.map { preferences ->
                val bandalartId = preferences[recentBandalartKey] ?: 0L
                if (bandalartId > 0L) preferences[recentSubGoalKey(bandalartId)] ?: 0L else 0L
            }.distinctUntilChanged()

    suspend fun setRecentBandalartId(recentBandalartId: Long) {
        dataStore.edit { preferences ->
            preferences[recentBandalartKey] = recentBandalartId
        }
    }

    suspend fun getRecentBandalartId(): Long = recentBandalartId.first()

    suspend fun setRecentSubGoalId(
        bandalartId: Long,
        subGoalId: Long,
    ) {
        dataStore.edit { preferences ->
            preferences[recentSubGoalKey(bandalartId)] = subGoalId
        }
    }

    suspend fun getRecentSubGoalId(bandalartId: Long): Long =
        dataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            }.first()[recentSubGoalKey(bandalartId)] ?: 0L

    private fun recentSubGoalKey(bandalartId: Long) = longPreferencesKey("$RECENT_SUB_GOAL_ID_PREFIX$bandalartId")

    private fun Preferences.pendingRewardedCreation(): StoredPendingRewardedCreation? {
        val requestId = this[pendingRewardedRequestIdKey] ?: return null
        val targetSlots = this[pendingRewardedTargetSlotsKey] ?: return null
        return StoredPendingRewardedCreation(
            requestId = requestId,
            targetSlots = targetSlots,
            isGranted = this[pendingRewardedGrantedKey] ?: false,
            templateId = this[pendingRewardedTemplateIdKey],
        )
    }

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

    suspend fun setThemeMode(themeMode: String) {
        dataStore.edit { preferences ->
            preferences[themeModeKey] = themeMode
        }
    }

    suspend fun setDeadlineReminderEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[deadlineReminderEnabledKey] = enabled
        }
    }

    suspend fun addRecentEmoji(emoji: String) {
        dataStore.edit { preferences ->
            val currentEmojis =
                preferences[recentEmojisKey]
                    ?.let { Json.decodeFromString<List<String>>(it) }
                    ?: emptyList()
            val updatedEmojis =
                (listOf(emoji) + currentEmojis.filterNot { it == emoji })
                    .take(MAX_RECENT_EMOJIS)
            preferences[recentEmojisKey] = Json.encodeToString(updatedEmojis)
        }
    }
}

data class StoredPendingRewardedCreation(
    val requestId: Long,
    val targetSlots: Int,
    val isGranted: Boolean,
    val templateId: String? = null,
)
