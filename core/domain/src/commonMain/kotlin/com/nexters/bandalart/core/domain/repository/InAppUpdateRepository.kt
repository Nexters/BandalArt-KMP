package com.nexters.bandalart.core.domain.repository

interface InAppUpdateRepository {
    /**
     * 인앱 업데이트 거절 버전 저장 (다시 보지 않기)
     *
     * @param rejectedVersionCode 거절한 버전 코드
     */
    suspend fun setLastRejectedUpdateVersion(rejectedVersionCode: Int)

    /**
     * 거절 했던 업데이트 버전인지 확인
     *
     * @param updateVersionCode 확인할 업데이트 버전 코드
     * @return 거절한 버전이면 true, 아니면 false
     */
    suspend fun isUpdateAlreadyRejected(updateVersionCode: Int): Boolean
}
