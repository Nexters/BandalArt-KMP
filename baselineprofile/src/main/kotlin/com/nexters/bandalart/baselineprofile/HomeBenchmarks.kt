package com.nexters.bandalart.baselineprofile

import android.os.SystemClock
import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

internal const val HOME_SCROLL_TAG = "home_scroll"
internal const val WAIT_TIMEOUT_MILLIS = 10_000L

@RunWith(AndroidJUnit4::class)
@LargeTest
class HomeBenchmarks {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun homeScrollFrameTiming() {
        rule.measureRepeated(
            packageName = targetAppId(),
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
            startupMode = StartupMode.WARM,
            iterations = 5,
            setupBlock = {
                pressHome()
                startTargetActivity()
                navigateToHomeIfNeeded()
            },
        ) {
            val homeScroll = device.wait(Until.findObject(By.res(HOME_SCROLL_TAG)), WAIT_TIMEOUT_MILLIS)
                ?: throw AssertionError("Home scroll target was not found")
            homeScroll.setGestureMargin(device.displayWidth / 5)
            homeScroll.fling(Direction.DOWN)
            homeScroll.fling(Direction.UP)
        }
    }
}

internal fun MacrobenchmarkScope.startTargetActivity() {
    device.executeShellCommand("am start -W -n ${targetAppId()}/com.nexters.bandalart.MainActivity")
}

internal fun MacrobenchmarkScope.navigateToHomeIfNeeded() {
    if (device.wait(Until.hasObject(By.res(HOME_SCROLL_TAG)), WAIT_TIMEOUT_MILLIS)) return

    repeat(4) {
        device.swipe(
            device.displayWidth - 50,
            device.displayHeight / 2,
            50,
            device.displayHeight / 2,
            50,
        )
        device.waitForIdle()
        SystemClock.sleep(500)
    }
    device.click(device.displayWidth / 2, device.displayHeight * 7 / 8)

    if (!device.wait(Until.hasObject(By.res(HOME_SCROLL_TAG)), WAIT_TIMEOUT_MILLIS)) {
        throw AssertionError("Home scroll target was not found after onboarding")
    }
}

internal fun targetAppId(): String =
    InstrumentationRegistry.getArguments().getString("targetAppId")
        ?: throw Exception("targetAppId not passed as instrumentation runner arg")
