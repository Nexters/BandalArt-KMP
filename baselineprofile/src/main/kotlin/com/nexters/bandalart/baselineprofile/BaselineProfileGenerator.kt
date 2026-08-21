package com.nexters.bandalart.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        rule.collect(
            packageName = targetAppId(),
            includeInStartupProfile = true,
        ) {
            pressHome()
            startTargetActivity()
            navigateToHomeIfNeeded()
            val homeScroll = device.wait(Until.findObject(By.res(HOME_SCROLL_TAG)), WAIT_TIMEOUT_MILLIS)
                ?: throw AssertionError("Home scroll target was not found")
            homeScroll.setGestureMargin(device.displayWidth / 5)
            homeScroll.fling(Direction.DOWN)
            homeScroll.fling(Direction.UP)
        }
    }
}
