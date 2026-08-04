package com.nexters.bandalart.feature.home.presenter

import com.nexters.bandalart.feature.home.HomeScreen
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HomePresenterTest {
    @Test
    fun `loads the most recently opened bandalart`() = runTest {
        val repository = FakeBandalartRepository(
            bandalarts = listOf(bandalart(1L), bandalart(2L)),
            recentBandalartId = 2L,
        )
        val presenter = HomePresenter(
            navigator = FakeNavigator(HomeScreen),
            bandalartRepository = repository,
            inAppUpdateRepository = FakeInAppUpdateRepository(),
        )

        presenter.test {
            var state = awaitItem()
            while (state.bandalartData?.id != 2L) {
                state = awaitItem()
            }

            assertEquals(2, state.bandalartList.size)
            assertEquals(2L, state.bandalartData?.id)
        }
    }

    @Test
    fun `creates a bandalart when the list is empty`() = runTest {
        val repository = FakeBandalartRepository(
            bandalarts = emptyList(),
            createdBandalart = bandalart(1L),
        )
        val presenter = HomePresenter(
            navigator = FakeNavigator(HomeScreen),
            bandalartRepository = repository,
            inAppUpdateRepository = FakeInAppUpdateRepository(),
        )

        presenter.test {
            var state = awaitItem()
            repository.awaitCreate()
            while (state.sideEffect !is HomeScreen.SideEffect.ShowSnackbar) {
                state = awaitItem()
            }

            assertEquals(1, repository.createCalls)
            assertEquals(1L, repository.recentBandalartId)
            assertEquals(1L, state.bandalartData?.id)
        }
    }

    @Test
    fun `does not create more than five bandalarts`() = runTest {
        val repository = FakeBandalartRepository(
            bandalarts = List(5) { index -> bandalart(index + 1L) },
        )
        val presenter = HomePresenter(
            navigator = FakeNavigator(HomeScreen),
            bandalartRepository = repository,
            inAppUpdateRepository = FakeInAppUpdateRepository(),
        )

        presenter.test {
            var state = awaitItem()
            while (state.bandalartList.size != 5) {
                state = awaitItem()
            }

            state.eventSink(HomeScreen.Event.OnAddClick)
            while (state.sideEffect !is HomeScreen.SideEffect.ShowToast) {
                state = awaitItem()
            }

            assertEquals(0, repository.createCalls)
        }
    }

    @Test
    fun `canceled update is not offered again`() = runTest {
        val updateRepository = FakeInAppUpdateRepository()
        val presenter = HomePresenter(
            navigator = FakeNavigator(HomeScreen),
            bandalartRepository = FakeBandalartRepository(listOf(bandalart(1L))),
            inAppUpdateRepository = updateRepository,
        )

        presenter.test {
            var state = awaitItem()
            while (state.bandalartCellData == null) {
                state = awaitItem()
            }

            state.eventSink(HomeScreen.Event.OnUpdateCheck(20206))
            while (state.updateVersionCode != 20206) {
                state = awaitItem()
            }

            state.eventSink(HomeScreen.Event.OnUpdateCanceled)
            while (state.updateVersionCode != null) {
                state = awaitItem()
            }

            assertEquals(20206, updateRepository.rejectedVersionCode)

            state.eventSink(HomeScreen.Event.OnUpdateCheck(20206))
            expectNoEvents()
        }
    }
}
