package com.nexters.bandalart.feature.complete.presenter

import android.net.Uri
import com.nexters.bandalart.feature.complete.CompleteScreen
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CompletePresenterTest {
    private val screen = CompleteScreen(
        bandalartId = 42L,
        bandalartTitle = "출시 준비",
        bandalartProfileEmoji = "🚀",
        bandalartChartImageUri = "content://bandalart/chart",
    )

    @Test
    fun `screen data is exposed and bandalart is marked complete`() = runTest {
        val repository = RecordingBandalartRepository()
        val presenter = CompletePresenter(FakeNavigator(screen), screen, repository)

        presenter.test {
            val state = awaitItem()

            assertEquals(screen.bandalartId, state.id)
            assertEquals(screen.bandalartTitle, state.title)
            assertEquals(screen.bandalartProfileEmoji, state.profileEmoji)
            assertEquals(screen.bandalartChartImageUri, state.bandalartChartImageUri)

            repository.awaitCompletion()
            assertEquals(screen.bandalartId to true, repository.completion)
        }
    }

    @Test
    fun `save and share events are exposed as side effects`() = runTest {
        val presenter = CompletePresenter(
            navigator = FakeNavigator(screen),
            screen = screen,
            bandalartRepository = RecordingBandalartRepository(),
        )
        val imageUri = Uri.parse(screen.bandalartChartImageUri)

        presenter.test {
            var state = awaitItem()

            state.eventSink(CompleteScreen.Event.SaveBandalart(imageUri))
            state = awaitItem()
            assertEquals(CompleteScreen.SideEffect.SaveImage(imageUri), state.sideEffect)

            state.eventSink(CompleteScreen.Event.ShareBandalart(imageUri))
            state = awaitItem()
            assertEquals(CompleteScreen.SideEffect.ShareImage(imageUri), state.sideEffect)

            state.eventSink(CompleteScreen.Event.InitSideEffect)
            assertNull(awaitItem().sideEffect)
        }
    }

    @Test
    fun `navigate back pops current screen`() = runTest {
        val navigator = FakeNavigator(screen)
        val presenter = CompletePresenter(navigator, screen, RecordingBandalartRepository())

        presenter.test {
            awaitItem().eventSink(CompleteScreen.Event.NavigateBack)

            assertTrue(navigator.awaitPop().result == null)
        }
    }
}
