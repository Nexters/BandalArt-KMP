package com.nexters.bandalart.feature.home.viewmodel

import android.content.res.Resources
import androidx.compose.ui.graphics.ImageBitmap
import com.nexters.bandalart.core.common.Language
import com.nexters.bandalart.core.common.Locale
import com.nexters.bandalart.core.domain.entity.BandalartCellEntity
import com.nexters.bandalart.core.domain.entity.BandalartEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartMainCellEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartSubCellEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartTaskCellEntity
import com.nexters.bandalart.core.domain.repository.BandalartRepository
import com.nexters.bandalart.core.domain.repository.InAppUpdateRepository
import com.nexters.bandalart.feature.home.model.CellType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("HomeViewModel 테스트")
class HomeViewModelTest {

    private lateinit var viewModel: HomeViewModel
    private lateinit var mockBandalartRepository: BandalartRepository
    private lateinit var mockInAppUpdateRepository: InAppUpdateRepository

    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockBandalartRepository = mockk(relaxed = true)
        mockInAppUpdateRepository = mockk(relaxed = true)

        // 기본 모킹 설정
        setupDefaultMocks()

        viewModel = HomeViewModel(mockBandalartRepository, mockInAppUpdateRepository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun setupDefaultMocks() {
        // 반다라트 목록 모킹
        coEvery { mockBandalartRepository.getBandalartList() } returns flowOf(
            listOf(
                createBandalartEntity(1),
                createBandalartEntity(2),
            ),
        )

        // 최근 반다라트 ID 모킹
        coEvery { mockBandalartRepository.getRecentBandalartId() } returns 1L

        // 특정 반다라트 조회 모킹
        coEvery { mockBandalartRepository.getBandalart(any()) } returns createBandalartEntity(1)

        // 메인 셀 조회 모킹
        coEvery { mockBandalartRepository.getBandalartMainCell(any()) } returns createBandalartCellEntity(1)

        // 하위 셀 조회 모킹
        coEvery { mockBandalartRepository.getChildCells(any()) } returns listOf(
            createBandalartCellEntity(2),
            createBandalartCellEntity(3),
        )
    }

    private fun createBandalartEntity(id: Long) = BandalartEntity(
        id = id,
        title = "반다라트 $id",
        description = "설명 $id",
        dueDate = "2025-04-15",
        profileEmoji = "😊",
        mainColor = "#3FFFBA",
        subColor = "#111827",
        isCompleted = false,
        completionRatio = 0,
    )

    private fun createBandalartCellEntity(id: Long) = BandalartCellEntity(
        id = id,
        title = "셀 $id",
        description = "설명 $id",
        dueDate = "2025-04-15",
        isCompleted = false,
        parentId = if (id == 1L) null else 1L,
        children = emptyList(),
    )

    @Nested
    @DisplayName("초기화")
    inner class Initialization {

        @Test
        @DisplayName("초기화 시 반다라트 목록을 불러와야 한다")
        fun testInitializeLoadsData() = runTest {
            // then
            coVerify { mockBandalartRepository.getBandalartList() }
            coVerify { mockBandalartRepository.getRecentBandalartId() }
            coVerify { mockBandalartRepository.getBandalart(1L) }
            coVerify { mockBandalartRepository.getBandalartMainCell(1L) }
        }

        @Test
        @DisplayName("반다라트 목록이 비어있을 때 새 반다라트를 생성해야 한다")
        fun testCreateBandalartWhenListEmpty() = runTest {
            // given
            coEvery { mockBandalartRepository.getBandalartList() } returns flowOf(emptyList())
            coEvery { mockBandalartRepository.createBandalart() } returns createBandalartEntity(1)

            // when
            val viewModel = HomeViewModel(mockBandalartRepository, mockInAppUpdateRepository)
            testScheduler.advanceUntilIdle()

            // then
            coVerify { mockBandalartRepository.createBandalart() }
        }
    }

    @Nested
    @DisplayName("반다라트 생성 및 관리")
    inner class BandalartManagement {

        @Test
        @DisplayName("새로운 반다라트를 생성할 수 있다")
        fun testCreateBandalart() = runTest {
            // given
            val newBandalart = createBandalartEntity(3)
            coEvery { mockBandalartRepository.createBandalart() } returns newBandalart

            // collect events
            val events = mutableListOf<HomeUiEvent>()
            val job = launch { viewModel.uiEvent.toList(events) }

            // when
            viewModel.onAction(HomeUiAction.OnAddClick)
            testScheduler.advanceUntilIdle()

            // then
            coVerify { mockBandalartRepository.createBandalart() }
            coVerify { mockBandalartRepository.setRecentBandalartId(3L) }
            assertTrue(events.any { it is HomeUiEvent.ShowSnackbar })

            job.cancel()
        }

        @Test
        @DisplayName("반다라트 목록이 5개를 초과하면 생성이 제한된다")
        fun testBandalartCreationLimit() = runTest {
            // given
            val fullList = List(5) { createBandalartEntity(it.toLong() + 1) }
            coEvery { mockBandalartRepository.getBandalartList() } returns flowOf(fullList)

            // recreate viewModel with updated mocks
            viewModel = HomeViewModel(mockBandalartRepository, mockInAppUpdateRepository)
            testScheduler.advanceUntilIdle()

            // collect events
            val events = mutableListOf<HomeUiEvent>()
            val job = launch { viewModel.uiEvent.toList(events) }

            // when
            viewModel.onAction(HomeUiAction.OnAddClick)
            testScheduler.advanceUntilIdle()

            // then
            coVerify(exactly = 0) { mockBandalartRepository.createBandalart() }
            assertTrue(events.any { it is HomeUiEvent.ShowToast })

            job.cancel()
        }

        @Test
        @DisplayName("반다라트를 삭제할 수 있다")
        fun testDeleteBandalart() = runTest {
            // given
            val bandalartId = 1L

            // collect events
            val events = mutableListOf<HomeUiEvent>()
            val job = launch { viewModel.uiEvent.toList(events) }

            // when
            // 삭제 대화상자 표시
            viewModel.onAction(HomeUiAction.OnDeleteButtonClick)
            testScheduler.advanceUntilIdle()
            // 실제 삭제 실행
            viewModel.onAction(HomeUiAction.OnDeleteBandalart(bandalartId))
            testScheduler.advanceUntilIdle()

            // then
            coVerify { mockBandalartRepository.deleteBandalart(bandalartId) }
            coVerify { mockBandalartRepository.deleteCompletedBandalartId(bandalartId) }
            assertTrue(events.any { it is HomeUiEvent.ShowSnackbar })

            job.cancel()
        }
    }

    @Nested
    @DisplayName("셀 업데이트 기능")
    inner class CellUpdates {

        @Test
        @DisplayName("메인 셀을 업데이트할 수 있다")
        fun testUpdateMainCell() = runTest {
            // given
            val cellData = createBandalartCellEntity(1).copy(
                title = "메인 목표",
                description = "메인 설명",
            )
            val bandalartData = createBandalartEntity(1)

            // when: 셀 클릭
            viewModel.onAction(
                HomeUiAction.OnBandalartCellClick(
                    cellType = CellType.MAIN,
                    isMainCellTitleEmpty = false,
                    cellData = cellData,
                ),
            )
            testScheduler.advanceUntilIdle()

            // then: 바텀시트가 표시되고 셀 데이터가 설정되어야 함
            assertTrue(viewModel.uiState.value.bottomSheet is BottomSheetState.Cell)
            assertEquals(cellData, (viewModel.uiState.value.bottomSheet as BottomSheetState.Cell).cellData)

            // when: 타이틀 업데이트
            viewModel.onAction(
                HomeUiAction.OnCellTitleUpdate(
                    "새 메인 목표",
                    object : Locale {
                        override val language = Language.KOREAN
                    },
                ),
            )

            // when: 완료 버튼 클릭
            viewModel.onAction(
                HomeUiAction.OnCompleteButtonClick(
                    bandalartId = 1L,
                    cellId = 1L,
                    cellType = CellType.MAIN,
                ),
            )
            testScheduler.advanceUntilIdle()

            // then: 업데이트 메소드가 호출되어야 함
            val mainCellModelSlot = slot<UpdateBandalartMainCellEntity>()
            coVerify { mockBandalartRepository.updateBandalartMainCell(1L, 1L, capture(mainCellModelSlot)) }

            assertEquals("새 메인 목표", mainCellModelSlot.captured.title)
        }

        @Test
        @DisplayName("서브 셀을 업데이트할 수 있다")
        fun testUpdateSubCell() = runTest {
            // given
            val cellData = createBandalartCellEntity(2).copy(
                title = "서브 목표",
                description = "서브 설명",
            )

            // when: 셀 클릭
            viewModel.onAction(
                HomeUiAction.OnBandalartCellClick(
                    cellType = CellType.SUB,
                    isMainCellTitleEmpty = false,
                    cellData = cellData,
                ),
            )
            testScheduler.advanceUntilIdle()

            // when: 타이틀 업데이트 및 완료 버튼 클릭
            viewModel.onAction(
                HomeUiAction.OnCellTitleUpdate(
                    "새 메인 목표",
                    object : Locale {
                        override val language = Language.KOREAN
                    },
                ),
            )
            viewModel.onAction(
                HomeUiAction.OnCompleteButtonClick(
                    bandalartId = 1L,
                    cellId = 2L,
                    cellType = CellType.SUB,
                ),
            )
            testScheduler.advanceUntilIdle()

            // then: 업데이트 메소드가 호출되어야 함
            val subCellModelSlot = slot<UpdateBandalartSubCellEntity>()
            coVerify { mockBandalartRepository.updateBandalartSubCell(1L, 2L, capture(subCellModelSlot)) }

            assertEquals("새 서브 목표", subCellModelSlot.captured.title)
        }

        @Test
        @DisplayName("태스크 셀을 업데이트하고 완료 상태를 변경할 수 있다")
        fun testUpdateTaskCell() = runTest {
            // given
            val cellData = createBandalartCellEntity(3).copy(
                title = "태스크 목표",
                description = "태스크 설명",
                isCompleted = false,
            )

            // when: 셀 클릭
            viewModel.onAction(
                HomeUiAction.OnBandalartCellClick(
                    cellType = CellType.TASK,
                    isMainCellTitleEmpty = false,
                    cellData = cellData,
                ),
            )
            testScheduler.advanceUntilIdle()

            // when: 타이틀, 완료 상태 업데이트 및 완료 버튼 클릭
            viewModel.onAction(
                HomeUiAction.OnCellTitleUpdate(
                    "새 메인 목표",
                    object : Locale {
                        override val language = Language.KOREAN
                    },
                ),
            )
            viewModel.onAction(HomeUiAction.OnCompletionUpdate(true))
            viewModel.onAction(
                HomeUiAction.OnCompleteButtonClick(
                    bandalartId = 1L,
                    cellId = 3L,
                    cellType = CellType.TASK,
                ),
            )
            testScheduler.advanceUntilIdle()

            // then: 업데이트 메소드가 호출되어야 함
            val taskCellModelSlot = slot<UpdateBandalartTaskCellEntity>()
            coVerify { mockBandalartRepository.updateBandalartTaskCell(1L, 3L, capture(taskCellModelSlot)) }

            assertEquals("새 태스크 목표", taskCellModelSlot.captured.title)
            assertTrue(taskCellModelSlot.captured.isCompleted!!)
        }
    }

    @Nested
    @DisplayName("UI 상태 관리")
    inner class UiStateManagement {

        @Test
        @DisplayName("드롭다운 메뉴를 토글할 수 있다")
        fun testToggleDropdownMenu() = runTest {
            // given
            assertFalse(viewModel.uiState.value.isDropDownMenuOpened)

            // when: 메뉴 버튼 클릭
            viewModel.onAction(HomeUiAction.OnMenuClick)

            // then: 메뉴가 열려야 함
            assertTrue(viewModel.uiState.value.isDropDownMenuOpened)

            // when: 메뉴 닫기
            viewModel.onAction(HomeUiAction.OnDropDownMenuDismiss)

            // then: 메뉴가 닫혀야 함
            assertFalse(viewModel.uiState.value.isDropDownMenuOpened)
        }

        @Test
        @DisplayName("이모지를 선택할 수 있다")
        fun testEmojiSelection() = runTest {
            // given: 테스트 스케줄러 준비
            testScheduler.advanceUntilIdle() // 초기화 동작 완료 대기

            // 현재 상태 확인
            assertNotNull(viewModel.uiState.value.bandalartData, "BandalartData should be initialized")

            // when: 셀 클릭으로 바텀시트 열기
            viewModel.onAction(
                HomeUiAction.OnBandalartCellClick(
                    cellType = CellType.MAIN,
                    isMainCellTitleEmpty = false,
                    cellData = createBandalartCellEntity(1),
                ),
            )
            testScheduler.advanceUntilIdle()

            // 바텀시트 상태 확인
            val bottomSheet = viewModel.uiState.value.bottomSheet
            val cellBottomSheet = bottomSheet as? BottomSheetState.Cell
            assertNotNull(cellBottomSheet, "Expected BottomSheetState.Cell but was ${bottomSheet?.javaClass?.name}")

            // when: 이모지 선택기 열기
            viewModel.onAction(HomeUiAction.OnEmojiPickerClick)
            testScheduler.advanceUntilIdle()

            // then: 이모지 선택기가 열려야 함
            val updatedBottomSheet = viewModel.uiState.value.bottomSheet as? BottomSheetState.Cell
            assertNotNull(updatedBottomSheet)
            assertTrue(updatedBottomSheet!!.isEmojiPickerOpened)

            // when: 이모지 선택
            viewModel.onAction(HomeUiAction.OnEmojiSelect("🚀"))
            testScheduler.advanceUntilIdle()

            // then: 이모지가 업데이트되고 선택기가 닫혀야 함
            val finalBottomSheet = viewModel.uiState.value.bottomSheet as? BottomSheetState.Cell
            assertNotNull(finalBottomSheet)
            assertEquals("🚀", finalBottomSheet!!.bandalartData.profileEmoji)
            assertFalse(finalBottomSheet.isEmojiPickerOpened)
        }

        @Test
        @DisplayName("바텀시트를 닫을 수 있다")
        fun testCloseBottomSheet() = runTest {
            // given: 초기화 동작 완료 대기
            testScheduler.advanceUntilIdle()

            // 현재 상태 확인
            assertNotNull(viewModel.uiState.value.bandalartData, "BandalartData should be initialized")

            // 바텀시트 표시
            viewModel.onAction(
                HomeUiAction.OnBandalartCellClick(
                    cellType = CellType.MAIN,
                    isMainCellTitleEmpty = false,
                    cellData = createBandalartCellEntity(1),
                ),
            )

            testScheduler.advanceUntilIdle()

            assertNotNull(viewModel.uiState.value.bottomSheet, "BottomSheet should be opened after cell click")

            // when: 닫기 버튼 클릭
            viewModel.onAction(HomeUiAction.OnCloseButtonClick)
            testScheduler.advanceUntilIdle()

            // then: 바텀시트가 닫혀야 함
            assertNull(viewModel.uiState.value.bottomSheet)
        }
    }

    @Nested
    @DisplayName("인앱 업데이트")
    inner class InAppUpdate {

        @Test
        @DisplayName("업데이트 거부 상태를 저장할 수 있다")
        fun testSetLastRejectedUpdateVersion() = runTest {
            // when
            viewModel.setLastRejectedUpdateVersion(20200)

            // then
            coVerify { mockInAppUpdateRepository.setLastRejectedUpdateVersion(20200) }
        }

        @Test
        @DisplayName("이미 거부된 업데이트인지 확인할 수 있다")
        fun testIsUpdateAlreadyRejected() = runTest {
            // given
            coEvery { mockInAppUpdateRepository.isUpdateAlreadyRejected(any()) } returns true

            // when
            val result = viewModel.isUpdateAlreadyRejected(20200)

            // then
            assertTrue(result)
            coVerify { mockInAppUpdateRepository.isUpdateAlreadyRejected(20200) }
        }
    }

    @Nested
    @DisplayName("이미지 캡처 및 공유")
    inner class ImageCaptureAndShare {

        @Test
        @DisplayName("반다라트 차트를 캡처할 수 있다")
        fun testCaptureBandalart() = runTest {
            // given
            val mockImageBitmap = mockk<ImageBitmap>()

            // collect events
            val events = mutableListOf<HomeUiEvent>()
            val job = launch { viewModel.uiEvent.toList(events) }

            // when: 캡처 요청
            viewModel.onAction(HomeUiAction.OnSaveClick)
            testScheduler.advanceUntilIdle()

            // then: 캡처 상태가 true로 설정됨
            assertTrue(viewModel.uiState.value.isCapturing)

            // when: 캡처 완료
            viewModel.captureBandalart(mockImageBitmap)
            testScheduler.advanceUntilIdle()

            // then: 캡처 상태가 false로 변경됨
            assertFalse(viewModel.uiState.value.isCapturing)
            assertTrue(events.any { it is HomeUiEvent.CaptureBandalart })

            job.cancel()
        }

        @Test
        @DisplayName("반다라트 차트를 공유할 수 있다")
        fun testShareBandalart() = runTest {
            // given
            val mockImageBitmap = mockk<ImageBitmap>()

            // collect events
            val events = mutableListOf<HomeUiEvent>()
            val job = launch { viewModel.uiEvent.toList(events) }

            // when: 공유 요청
            viewModel.onAction(HomeUiAction.OnShareButtonClick)
            testScheduler.advanceUntilIdle()

            // then: 공유 상태가 true로 설정됨
            assertTrue(viewModel.uiState.value.isSharing)

            // when: 공유 실행
            viewModel.shareBandalart(mockImageBitmap)
            testScheduler.advanceUntilIdle()

            // then: 공유 상태가 false로 변경됨
            assertFalse(viewModel.uiState.value.isSharing)
            assertTrue(events.any { it is HomeUiEvent.ShareBandalart })

            job.cancel()
        }
    }
}
