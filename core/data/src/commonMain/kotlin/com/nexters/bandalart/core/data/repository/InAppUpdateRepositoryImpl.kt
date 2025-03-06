package com.nexters.bandalart.core.data.repository

import com.nexters.bandalart.core.datastore.InAppUpdateDataStore
import com.nexters.bandalart.core.domain.repository.InAppUpdateRepository

class InAppUpdateRepositoryImpl(
    private val inAppUpdateDataStore: InAppUpdateDataStore,
) : InAppUpdateRepository {
    override suspend fun setLastRejectedUpdateVersion(rejectedVersionCode: Int) {
        inAppUpdateDataStore.setLastRejectedUpdateVersion(rejectedVersionCode)
    }

    override suspend fun isUpdateAlreadyRejected(updateVersionCode: Int): Boolean {
        val lastRejectedUpdateVersion = inAppUpdateDataStore.getLastRejectedUpdateVersion()
        return updateVersionCode <= lastRejectedUpdateVersion
    }
}
