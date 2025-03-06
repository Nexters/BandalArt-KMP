package com.nexters.bandalart.core.domain.repository

interface OnboardingRepository {
    /**
     * 온보딩 완료 여부 저장
     *
     * @param flag true(완료), false(진행중)
     */
    suspend fun setOnboardingCompletedStatus(flag: Boolean)

    /**
     * 온보딩 완료 여부 조회
     */
    suspend fun getOnboardingCompletedStatus(): Boolean
}
