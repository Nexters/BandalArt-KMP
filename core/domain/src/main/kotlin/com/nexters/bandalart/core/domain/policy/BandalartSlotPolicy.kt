package com.nexters.bandalart.core.domain.policy

const val FREE_BANDALART_SLOT_COUNT = 3

fun resolveMaxBandalartSlots(currentBandalartCount: Int): Int =
    maxOf(FREE_BANDALART_SLOT_COUNT, currentBandalartCount)

fun canCreateBandalart(
    currentBandalartCount: Int,
    maxBandalartSlots: Int,
): Boolean = currentBandalartCount < maxBandalartSlots
