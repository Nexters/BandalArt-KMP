package com.nexters.bandalart.feature.home.presenter

import com.nexters.bandalart.core.domain.repository.InAppUpdateRepository

internal class FakeInAppUpdateRepository : InAppUpdateRepository {
    var rejectedVersionCode: Int? = null
        private set

    override suspend fun setLastRejectedUpdateVersion(rejectedVersionCode: Int) {
        this.rejectedVersionCode = rejectedVersionCode
    }

    override suspend fun isUpdateAlreadyRejected(updateVersionCode: Int): Boolean {
        return rejectedVersionCode == updateVersionCode
    }
}
