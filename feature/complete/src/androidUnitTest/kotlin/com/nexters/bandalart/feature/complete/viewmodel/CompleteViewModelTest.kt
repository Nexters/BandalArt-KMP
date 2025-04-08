package com.nexters.bandalart.feature.complete.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import app.cash.turbine.test
import com.eygraber.uri.Uri
import com.nexters.bandalart.core.domain.repository.BandalartRepository
import com.nexters.bandalart.core.navigation.Route
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("CompleteViewModel 테스트")
class CompleteViewModelTest {

    private lateinit var completeViewModel: CompleteViewModel
    private lateinit var mockBandalartRepository: BandalartRepository
    private lateinit var mockSavedStateHandle: SavedStateHandle
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockBandalartRepository = mockk()
        mockSavedStateHandle = mockk()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    @DisplayName("ViewModel 초기화 시 상태가 올바르게 설정되어야 한다")
    fun testViewModelInitialization() = runTest {
        // given
        val bandalartId = 1L
        val bandalartTitle = "Test Bandalart"
        val profileEmoji = "😎"
        val chartImageUri = "content://test/image"

        coEvery {
            mockSavedStateHandle.toRoute<Route.Complete>()
        } returns Route.Complete(
            bandalartId = bandalartId,
            bandalartTitle = bandalartTitle,
            bandalartProfileEmoji = profileEmoji,
            bandalartChartImageUri = chartImageUri
        )
        coEvery {
            mockBandalartRepository.upsertBandalartId(bandalartId, true)
        } returns Unit

        // when
        completeViewModel = CompleteViewModel(mockBandalartRepository, mockSavedStateHandle)

        // then
        completeViewModel.uiState.test {
            val state = awaitItem()
            assertEquals(bandalartId, state.id)
            assertEquals(bandalartTitle, state.title)
            assertEquals(profileEmoji, state.profileEmoji)
            assertEquals(chartImageUri, state.bandalartChartImageUri)
            cancelAndIgnoreRemainingEvents()
        }

        // verify
        coVerify {
            mockBandalartRepository.upsertBandalartId(bandalartId, true)
        }
    }

    @Test
    @DisplayName("뒤로가기 버튼 클릭 시 NavigateBack 이벤트가 발생해야 한다")
    fun testOnBackButtonClick() = runTest {
        // given
        setupMockSavedStateHandle()
        completeViewModel = CompleteViewModel(mockBandalartRepository, mockSavedStateHandle)

        // when
        completeViewModel.onAction(CompleteUiAction.OnBackButtonClick)

        // then
        completeViewModel.uiEvent.test {
            testScheduler.advanceUntilIdle()
            assertEquals(CompleteUiEvent.NavigateBack, awaitItem())
            awaitComplete()
        }
    }

    @Test
    @DisplayName("저장 버튼 클릭 시 SaveBandalart 이벤트가 발생해야 한다")
    fun testOnSaveButtonClick() = runTest {
        // given
        val chartImageUri = "content://test/image"
        coEvery {
            mockSavedStateHandle.toRoute<Route.Complete>()
        } returns Route.Complete(
            bandalartId = 1L,
            bandalartTitle = "Test",
            bandalartProfileEmoji = "😎",
            bandalartChartImageUri = chartImageUri
        )
        completeViewModel = CompleteViewModel(mockBandalartRepository, mockSavedStateHandle)

        // when
        completeViewModel.onAction(CompleteUiAction.OnSaveButtonClick)

        // then
        completeViewModel.uiEvent.test {
            testScheduler.advanceUntilIdle()
            val event = awaitItem()
            assert(event is CompleteUiEvent.SaveBandalart)
            assertEquals(Uri.parse(chartImageUri), (event as CompleteUiEvent.SaveBandalart).imageUri)
            awaitComplete()
        }
    }

    @Test
    @DisplayName("공유 버튼 클릭 시 ShareBandalart 이벤트가 발생해야 한다")
    fun testOnShareButtonClick() = runTest {
        // given
        val chartImageUri = "content://test/image"
        coEvery {
            mockSavedStateHandle.toRoute<Route.Complete>()
        } returns Route.Complete(
            bandalartId = 1L,
            bandalartTitle = "Test",
            bandalartProfileEmoji = "😎",
            bandalartChartImageUri = chartImageUri
        )
        completeViewModel = CompleteViewModel(mockBandalartRepository, mockSavedStateHandle)

        // when
        completeViewModel.onAction(CompleteUiAction.OnShareButtonClick)

        // then
        completeViewModel.uiEvent.test {
            testScheduler.advanceUntilIdle()
            val event = awaitItem()
            assert(event is CompleteUiEvent.ShareBandalart)
            assertEquals(Uri.parse(chartImageUri), (event as CompleteUiEvent.ShareBandalart).imageUri)
            awaitComplete()
        }
    }

    private fun setupMockSavedStateHandle() {
        coEvery {
            mockSavedStateHandle.toRoute<Route.Complete>()
        } returns Route.Complete(
            bandalartId = 1L,
            bandalartTitle = "Test",
            bandalartProfileEmoji = "😎",
            bandalartChartImageUri = "content://test/image"
        )
    }
}
