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

    /**
     * 온보딩의 달성 기록 소개 장을 봤다고 저장
     */
    suspend fun markOnboardingRecordIntroShown()

    /**
     * 온보딩의 달성 기록 소개 장을 본 적 있는지 조회
     */
    suspend fun getOnboardingRecordIntroShown(): Boolean
}
