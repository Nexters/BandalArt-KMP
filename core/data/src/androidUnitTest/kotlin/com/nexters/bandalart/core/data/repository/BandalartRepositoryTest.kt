/*
 * Copyright 2025 easyhooon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nexters.bandalart.core.data.repository

import com.nexters.bandalart.core.data.mapper.toDto
import com.nexters.bandalart.core.data.mapper.toEntity
import com.nexters.bandalart.core.database.BandalartDao
import com.nexters.bandalart.core.database.entity.BandalartCellDBEntity
import com.nexters.bandalart.core.database.entity.BandalartCellWithChildrenDto
import com.nexters.bandalart.core.database.entity.BandalartDBEntity
import com.nexters.bandalart.core.datastore.BandalartDataStore
import com.nexters.bandalart.core.domain.entity.UpdateBandalartEmojiEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartMainCellEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartSubCellEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartTaskCellEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("BandalartRepository 테스트")
class DefaultBandalartRepositoryTest {
    private lateinit var bandalartRepository: DefaultBandalartRepository
    private lateinit var mockBandalartDao: BandalartDao
    private lateinit var mockBandalartDataStore: BandalartDataStore

    @BeforeEach
    fun setUp() {
        mockBandalartDao = mockk()
        mockBandalartDataStore = mockk()
        bandalartRepository = DefaultBandalartRepository(mockBandalartDataStore, mockBandalartDao)
    }

    @Nested
    @DisplayName("반다라트 셀 업데이트 테스트")
    inner class BandalartCellUpdateTest {
        @Test
        @DisplayName("메인 셀을 전체 필드로 업데이트할 수 있어야 한다")
        fun testUpdateBandalartMainCell() =
            runTest {
                // given
                val bandalartId = 1L
                val cellId = 2L
                val updateEntity =
                    UpdateBandalartMainCellEntity(
                        title = "Updated Title",
                        description = "Updated Description",
                        dueDate = "2025-12-31T23:59:59",
                        profileEmoji = "😎",
                        mainColor = "#FF0000",
                        subColor = "#00FF00",
                    )

                coEvery { mockBandalartDao.updateMainCellWithDto(any(), any()) } returns Unit

                // when
                bandalartRepository.updateBandalartMainCell(bandalartId, cellId, updateEntity)

                // then
                coVerify { mockBandalartDao.updateMainCellWithDto(cellId, updateEntity.toDto()) }
            }

        @Test
        @DisplayName("서브 셀을 부분 필드로 업데이트할 수 있어야 한다")
        fun testUpdateBandalartSubCell() =
            runTest {
                // given
                val bandalartId = 1L
                val cellId = 2L
                val updateEntity =
                    UpdateBandalartSubCellEntity(
                        title = "Updated Sub Cell",
                        description = "Sub Cell Description",
                        dueDate = "2025-10-15T12:00:00",
                    )

                coEvery { mockBandalartDao.updateSubCellWithDto(any(), any()) } returns Unit

                // when
                bandalartRepository.updateBandalartSubCell(bandalartId, cellId, updateEntity)

                // then
                coVerify { mockBandalartDao.updateSubCellWithDto(cellId, updateEntity.toDto()) }
            }

        @Test
        @DisplayName("태스크 셀을 완료 상태와 함께 업데이트할 수 있어야 한다")
        fun testUpdateBandalartTaskCell() =
            runTest {
                // given
                val bandalartId = 1L
                val cellId = 2L
                val updateEntity =
                    UpdateBandalartTaskCellEntity(
                        title = "Task Cell Title",
                        description = "Task Description",
                        dueDate = "2025-08-01T10:00:00",
                        isCompleted = true,
                    )

                coEvery { mockBandalartDao.updateTaskCellWithDto(any(), any()) } returns Unit

                // when
                bandalartRepository.updateBandalartTaskCell(bandalartId, cellId, updateEntity)

                // then
                coVerify { mockBandalartDao.updateTaskCellWithDto(cellId, updateEntity.toDto()) }
            }

        @Test
        @DisplayName("반다라트 이모지를 업데이트할 수 있어야 한다")
        fun testUpdateBandalartEmoji() =
            runTest {
                // given
                val bandalartId = 1L
                val cellId = 2L
                val updateEntity = UpdateBandalartEmojiEntity(profileEmoji = "😎")

                coEvery { mockBandalartDao.updateEmojiWithDto(any(), any()) } returns Unit

                // when
                bandalartRepository.updateBandalartEmoji(bandalartId, cellId, updateEntity)

                // then
                coVerify { mockBandalartDao.updateEmojiWithDto(cellId, updateEntity.toDto()) }
            }
    }

    @Nested
    @DisplayName("반다라트 삭제 및 조회 테스트")
    inner class BandalartDeletionAndRetrievalTest {
        @Test
        @DisplayName("특정 반다라트를 삭제할 수 있어야 한다")
        fun testDeleteBandalart() =
            runTest {
                // given
                val bandalartId = 1L
                val mockMainCell =
                    mockk<BandalartCellDBEntity> {
                        every { id } returns 2L
                    }
                val mockMainCellWithChildren =
                    mockk<BandalartCellWithChildrenDto> {
                        every { cell } returns mockMainCell
                    }

                coEvery { mockBandalartDao.getBandalartMainCell(bandalartId) } returns mockMainCellWithChildren
                coEvery { mockBandalartDao.deleteCellOrReset(any()) } returns Unit

                // when
                bandalartRepository.deleteBandalart(bandalartId)

                // then
                coVerify { mockBandalartDao.getBandalartMainCell(bandalartId) }
                coVerify { mockBandalartDao.deleteCellOrReset(2L) }
            }

        @Test
        @DisplayName("특정 반다라트를 조회할 수 있어야 한다")
        fun testGetBandalart() =
            runTest {
                // given
                val bandalartId = 1L
                val mockBandalartDbEntity = BandalartDBEntity(id = bandalartId)

                coEvery { mockBandalartDao.getBandalart(bandalartId) } returns mockBandalartDbEntity

                // when
                val result = bandalartRepository.getBandalart(bandalartId)

                // then
                assertEquals(mockBandalartDbEntity.toEntity(), result)
                coVerify { mockBandalartDao.getBandalart(bandalartId) }
            }

        @Test
        @DisplayName("메인 셀을 조회할 수 있어야 한다")
        fun testGetBandalartMainCell() =
            runTest {
                // given
                val bandalartId = 1L
                val mockMainCell = BandalartCellDBEntity(id = 2L, bandalartId = bandalartId)
                val mockMainCellWithChildren =
                    BandalartCellWithChildrenDto(
                        cell = mockMainCell,
                        children = emptyList(),
                    )

                coEvery { mockBandalartDao.getBandalartMainCell(bandalartId) } returns mockMainCellWithChildren

                // when
                val result = bandalartRepository.getBandalartMainCell(bandalartId)

                // then
                assertEquals(mockMainCell.toEntity(), result)
                coVerify { mockBandalartDao.getBandalartMainCell(bandalartId) }
            }

        @Test
        @DisplayName("자식 셀들을 조회할 수 있어야 한다")
        fun testGetChildCells() =
            runTest {
                // given
                val parentId = 1L
                val mockChildCells =
                    listOf(
                        BandalartCellDBEntity(
                            id = 2L,
                            bandalartId = 1L,
                            parentId = parentId,
                            title = "Child Cell 1",
                            description = "Description 1",
                            dueDate = "2025-10-15T12:00:00",
                            isCompleted = false,
                        ),
                        BandalartCellDBEntity(
                            id = 3L,
                            bandalartId = 1L,
                            parentId = parentId,
                            title = "Child Cell 2",
                            description = "Description 2",
                            dueDate = "2025-11-20T15:30:00",
                            isCompleted = true,
                        ),
                    )

                coEvery { mockBandalartDao.getChildCells(parentId) } returns mockChildCells

                // when
                val result = bandalartRepository.getChildCells(parentId)

                // then
                assertEquals(mockChildCells.map { it.toEntity() }, result)
                coVerify { mockBandalartDao.getChildCells(parentId) }
            }
    }

    @Nested
    @DisplayName("이전 반다라트 관리 테스트")
    inner class PreviousBandalartManagementTest {
        @Test
        @DisplayName("이전 반다라트 목록을 조회할 수 있어야 한다")
        fun testGetPrevBandalartList() =
            runTest {
                // given
                val mockPrevBandalartList =
                    listOf(
                        Pair(1L, true),
                        Pair(2L, false),
                    )

                coEvery { mockBandalartDataStore.getPrevBandalartList() } returns mockPrevBandalartList

                // when
                val result = bandalartRepository.getPrevBandalartList()

                // then
                assertEquals(mockPrevBandalartList, result)
                coVerify { mockBandalartDataStore.getPrevBandalartList() }
            }

        @Test
        @DisplayName("반다라트 ID를 업서트할 수 있어야 한다")
        fun testUpsertBandalartId() =
            runTest {
                // given
                val bandalartId = 1L
                val isCompleted = true

                coEvery { mockBandalartDataStore.upsertBandalartId(bandalartId, isCompleted) } returns Unit

                // when
                bandalartRepository.upsertBandalartId(bandalartId, isCompleted)

                // then
                coVerify { mockBandalartDataStore.upsertBandalartId(bandalartId, isCompleted) }
            }

        @Test
        @DisplayName("완료된 반다라트 ID를 확인할 수 있어야 한다")
        fun testCheckCompletedBandalartId() =
            runTest {
                // given
                val bandalartId = 1L
                val isCompleted = true

                coEvery { mockBandalartDataStore.checkCompletedBandalartId(bandalartId) } returns isCompleted

                // when
                val result = bandalartRepository.checkCompletedBandalartId(bandalartId)

                // then
                assertEquals(isCompleted, result)
                coVerify { mockBandalartDataStore.checkCompletedBandalartId(bandalartId) }
            }

        @Test
        @DisplayName("완료된 반다라트 ID를 삭제할 수 있어야 한다")
        fun testDeleteCompletedBandalartId() =
            runTest {
                // given
                val bandalartId = 1L

                coEvery { mockBandalartDataStore.deleteBandalartId(bandalartId) } returns Unit

                // when
                bandalartRepository.deleteCompletedBandalartId(bandalartId)

                // then
                coVerify { mockBandalartDataStore.deleteBandalartId(bandalartId) }
            }
    }
}
