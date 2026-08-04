package com.nexters.bandalart.feature.splash.presenter

import com.nexters.bandalart.core.domain.repository.OnboardingRepository
import com.nexters.bandalart.feature.home.HomeScreen
import com.nexters.bandalart.feature.onboarding.OnboardingScreen
import com.nexters.bandalart.feature.splash.SplashScreen
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SplashPresenterTest {
    @Test
    fun `completed onboarding opens home`() = runTest {
        assertDestination(isCompleted = true, expected = HomeScreen)
    }

    @Test
    fun `incomplete onboarding opens onboarding`() = runTest {
        assertDestination(isCompleted = false, expected = OnboardingScreen)
    }

    private suspend fun assertDestination(isCompleted: Boolean, expected: Screen) {
        val repository = FakeOnboardingRepository(isCompleted)
        val navigator = FakeNavigator(SplashScreen)
        val presenter = SplashPresenter(navigator, repository)

        presenter.test {
            var state = awaitItem()
            if (state.isOnboardingCompleted != isCompleted) {
                state = awaitItem()
            }

            state.eventSink(SplashScreen.Event.CheckOnboardingStatus)

            assertEquals(expected, navigator.awaitResetRoot().newRoot)
        }
    }
}

private class FakeOnboardingRepository(isCompleted: Boolean) : OnboardingRepository {
    private val completed = MutableStateFlow(isCompleted)

    override suspend fun setOnboardingCompletedStatus(flag: Boolean) {
        completed.value = flag
    }

    override fun flowIsOnboardingCompleted(): Flow<Boolean> = completed
}
