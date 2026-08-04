package com.nexters.bandalart.feature.onboarding.presenter

import com.nexters.bandalart.feature.home.HomeScreen
import com.nexters.bandalart.feature.onboarding.OnboardingScreen
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingPresenterTest {
    @Test
    fun `complete onboarding persists status and opens home`() = runTest {
        val repository = FakeOnboardingRepository()
        val navigator = FakeNavigator(OnboardingScreen)
        val presenter = OnboardingPresenter(navigator, repository)

        presenter.test {
            awaitItem().eventSink(OnboardingScreen.Event.NavigateToHome)

            assertEquals(HomeScreen, navigator.awaitResetRoot().newRoot)
            assertTrue(repository.isCompleted)
        }
    }
}
