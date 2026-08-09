/*
 * Copyright 2026 easyhooon
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

package com.nexters.bandalart.di.metro

import com.nexters.bandalart.core.data.repository.DefaultBandalartRepository
import com.nexters.bandalart.core.data.repository.DefaultBandalartSlotRepository
import com.nexters.bandalart.core.data.notification.DefaultDeadlineReminderReconciler
import com.nexters.bandalart.core.data.repository.DefaultDeadlineReminderProjectionRepository
import com.nexters.bandalart.core.data.repository.DefaultInAppUpdateRepository
import com.nexters.bandalart.core.data.repository.DefaultOnboardingRepository
import com.nexters.bandalart.core.data.repository.DefaultSettingsRepository
import com.nexters.bandalart.core.database.BandalartDao
import com.nexters.bandalart.core.datastore.BandalartDataStore
import com.nexters.bandalart.core.datastore.InAppUpdateDataStore
import com.nexters.bandalart.core.domain.repository.BandalartRepository
import com.nexters.bandalart.core.domain.repository.BandalartSlotRepository
import com.nexters.bandalart.core.domain.repository.InAppUpdateRepository
import com.nexters.bandalart.core.domain.repository.OnboardingRepository
import com.nexters.bandalart.core.domain.repository.SettingsRepository
import com.nexters.bandalart.core.domain.notification.BufferedDeadlineNotificationLaunchTarget
import com.nexters.bandalart.core.domain.notification.DeadlineNotificationAuthorization
import com.nexters.bandalart.core.domain.notification.DeadlineNotificationLaunchTarget
import com.nexters.bandalart.core.domain.notification.DeadlineReminderPlanner
import com.nexters.bandalart.core.domain.notification.DeadlineReminderReconciler
import com.nexters.bandalart.core.domain.notification.DeadlineReminderScheduler
import com.nexters.bandalart.core.domain.notification.DeadlineReminderTimeZoneProvider
import com.nexters.bandalart.core.domain.repository.DeadlineReminderProjectionRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

@BindingContainer
object RepositoryBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun provideBandalartRepository(
        bandalartDataStore: BandalartDataStore,
        bandalartDao: BandalartDao,
        deadlineReminderReconciler: DeadlineReminderReconciler,
    ): BandalartRepository = DefaultBandalartRepository(bandalartDataStore, bandalartDao, deadlineReminderReconciler)

    @Provides
    @SingleIn(AppScope::class)
    fun provideBandalartSlotRepository(bandalartDataStore: BandalartDataStore): BandalartSlotRepository =
        DefaultBandalartSlotRepository(bandalartDataStore)

    @Provides
    @SingleIn(AppScope::class)
    fun provideInAppUpdateRepository(inAppUpdateDataStore: InAppUpdateDataStore): InAppUpdateRepository =
        DefaultInAppUpdateRepository(inAppUpdateDataStore)

    @Provides
    @SingleIn(AppScope::class)
    fun provideOnboardingRepository(bandalartDataStore: BandalartDataStore): OnboardingRepository = DefaultOnboardingRepository(bandalartDataStore)

    @Provides
    @SingleIn(AppScope::class)
    fun provideSettingsRepository(bandalartDataStore: BandalartDataStore): SettingsRepository = DefaultSettingsRepository(bandalartDataStore)

    @Provides
    @SingleIn(AppScope::class)
    fun provideDeadlineReminderProjectionRepository(bandalartDao: BandalartDao): DeadlineReminderProjectionRepository =
        DefaultDeadlineReminderProjectionRepository(bandalartDao)

    @Provides
    @SingleIn(AppScope::class)
    fun provideDeadlineReminderReconciler(
        settingsRepository: SettingsRepository,
        projectionRepository: DeadlineReminderProjectionRepository,
        scheduler: DeadlineReminderScheduler,
        authorization: DeadlineNotificationAuthorization,
    ): DeadlineReminderReconciler =
        DefaultDeadlineReminderReconciler(
            settingsRepository = settingsRepository,
            projectionRepository = projectionRepository,
            planner =
                DeadlineReminderPlanner(
                    clock = Clock.System,
                    timeZoneProvider = DeadlineReminderTimeZoneProvider(TimeZone::currentSystemDefault),
                ),
            scheduler = scheduler,
            authorization = authorization,
        )

    @Provides
    @SingleIn(AppScope::class)
    fun provideDeadlineNotificationLaunchTarget(): DeadlineNotificationLaunchTarget = BufferedDeadlineNotificationLaunchTarget()
}
