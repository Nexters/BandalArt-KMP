package com.nexters.bandalart.core.domain.policy

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class BandalartSlotPolicyTest : StringSpec({
    "기본 슬롯은 3개다" {
        resolveMaxBandalartSlots(currentBandalartCount = 0) shouldBe 3
    }

    "기존 사용자가 3개보다 많이 보유하면 현재 개수까지 보장한다" {
        resolveMaxBandalartSlots(currentBandalartCount = 5) shouldBe 5
    }

    "현재 개수가 최대 슬롯보다 적을 때만 바로 생성할 수 있다" {
        canCreateBandalart(currentBandalartCount = 2, maxBandalartSlots = 3) shouldBe true
        canCreateBandalart(currentBandalartCount = 3, maxBandalartSlots = 3) shouldBe false
    }
})
