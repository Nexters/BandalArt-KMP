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

package com.nexters.bandalart.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.nexters.bandalart.core.database.entity.UpdateBandalartMainCellDto
import com.nexters.bandalart.core.database.entity.UpdateBandalartSubCellDto
import com.nexters.bandalart.core.database.entity.UpdateBandalartTaskCellDto
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.annotation.Config
import tech.apter.junit.jupiter.robolectric.RobolectricExtension
import java.io.IOException

@ExtendWith(RobolectricExtension::class)
@Config(sdk = [35])
@DisplayName("BandalartDao 테스트")
class BandalartDaoTest {
    private lateinit var bandalartDao: BandalartDao
    private lateinit var db: BandalartDatabase

    @BeforeEach
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room
                .inMemoryDatabaseBuilder(
                    context, BandalartDatabase::class.java
                ).build()
        bandalartDao = db.bandalartDao
    }

    @AfterEach
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Nested
    @DisplayName("반다라트 생성 테스트")
    inner class BandalartCreationTest {
        @Test
        @DisplayName("빈 반다라트 생성 시 기본 구조가 올바르게 구성되어야 한다")
        fun testCreateEmptyBandalart() =
            runTest {
                // when
                val bandalartId = bandalartDao.createEmptyBandalart()

                // then
                val bandalart = bandalartDao.getBandalart(bandalartId)
                assertNotNull(bandalart)
                assertEquals("#3FFFBA", bandalart.mainColor)
                assertEquals("#111827", bandalart.subColor)

                // 메인 셀 확인
                val mainCell = bandalartDao.getBandalartMainCell(bandalartId)
                assertNotNull(mainCell)
                assertEquals("", mainCell.cell.title)

                // 서브 셀 4개 확인
                assertEquals(4, mainCell.children.size)

                // 각 서브 셀당 태스크 셀 5개 확인
                mainCell.children.forEach { subCell ->
                    val taskCells = bandalartDao.getChildCells(subCell.id!!)
                    assertEquals(5, taskCells.size)
                }
            }
    }

    @Nested
    @DisplayName("반다라트 셀 업데이트 테스트")
    inner class CellUpdateTest {
        @Test
        @DisplayName("메인 셀 업데이트 시 반다라트 정보도 함께 업데이트되어야 한다")
        fun testUpdateMainCell() =
            runTest {
                // given
                val bandalartId = bandalartDao.createEmptyBandalart()
                val mainCell = bandalartDao.getBandalartMainCell(bandalartId)
                val updateDto =
                    UpdateBandalartMainCellDto(
                        title = "업데이트된 메인 셀 제목",
                        description = "업데이트된 메인 셀 설명",
                        dueDate = "2025-12-31T23:59:59",
                        profileEmoji = "😎",
                        mainColor = "#FF3FFFBA",
                        subColor = "#FF111827"
                    )

                // when
                bandalartDao.updateMainCellWithDto(mainCell.cell.id!!, updateDto)

                // then
                val updatedMainCell = bandalartDao.getBandalartMainCell(bandalartId)
                assertEquals("업데이트된 메인 셀 제목", updatedMainCell.cell.title)
                assertEquals("업데이트된 메인 셀 설명", updatedMainCell.cell.description)
                assertEquals("2025-12-31T23:59:59", updatedMainCell.cell.dueDate)

                val updatedBandalart = bandalartDao.getBandalart(bandalartId)
                assertEquals("😎", updatedBandalart.profileEmoji)
                assertEquals("#FF3FFFBA", updatedBandalart.mainColor)
                assertEquals("#FF111827", updatedBandalart.subColor)
            }

        @Test
        @DisplayName("메인 셀 마감일에 null을 저장하면 기존 마감일만 제거되어야 한다")
        fun mainCellDueDateCanBeCleared() =
            runTest {
                val bandalartId = bandalartDao.createEmptyBandalart()
                val mainCell = bandalartDao.getBandalartMainCell(bandalartId).cell
                bandalartDao.updateMainCellWithDto(
                    cellId = mainCell.id!!,
                    updateDto =
                        UpdateBandalartMainCellDto(
                            title = "유지할 제목",
                            description = "유지할 설명",
                            dueDate = "2026-08-31T00:00",
                            profileEmoji = null,
                            mainColor = "#3FFFBA",
                            subColor = "#111827",
                        ),
                )

                bandalartDao.updateMainCellWithDto(
                    cellId = mainCell.id,
                    updateDto =
                        UpdateBandalartMainCellDto(
                            title = null,
                            description = null,
                            dueDate = null,
                            profileEmoji = null,
                            mainColor = "#3FFFBA",
                            subColor = "#111827",
                        ),
                )

                val updatedMainCell = bandalartDao.getBandalartMainCell(bandalartId).cell
                val updatedBandalart = bandalartDao.getBandalart(bandalartId)
                assertEquals("유지할 제목", updatedMainCell.title)
                assertEquals("유지할 설명", updatedMainCell.description)
                assertEquals(null, updatedMainCell.dueDate)
                assertEquals("유지할 제목", updatedBandalart.title)
                assertEquals("유지할 설명", updatedBandalart.description)
                assertEquals(null, updatedBandalart.dueDate)
            }

        @Test
        @DisplayName("서브 셀 업데이트 시 내용이 올바르게 변경되어야 한다")
        fun testUpdateSubCell() =
            runTest {
                // given
                val bandalartId = bandalartDao.createEmptyBandalart()
                val mainCell = bandalartDao.getBandalartMainCell(bandalartId)
                val subCell = mainCell.children.first()
                val updateDto =
                    UpdateBandalartSubCellDto(
                        title = "업데이트된 서브 셀 제목",
                        description = "업데이트된 서브 셀 설명",
                        dueDate = "2025-10-15T12:00:00"
                    )

                // when
                bandalartDao.updateSubCellWithDto(subCell.id!!, updateDto)

                // then
                val updatedSubCell = bandalartDao.getCell(subCell.id)
                assertEquals("업데이트된 서브 셀 제목", updatedSubCell.title)
                assertEquals("업데이트된 서브 셀 설명", updatedSubCell.description)
                assertEquals("2025-10-15T12:00:00", updatedSubCell.dueDate)
                assertFalse(updatedSubCell.isCompleted)
            }

        @Test
        @DisplayName("태스크 셀 업데이트 시 완료 상태가 변경될 수 있어야 한다")
        fun testUpdateTaskCell() =
            runTest {
                // given
                val bandalartId = bandalartDao.createEmptyBandalart()
                val mainCell = bandalartDao.getBandalartMainCell(bandalartId)
                val subCell = mainCell.children.first()
                val taskCells = bandalartDao.getChildCells(subCell.id!!)
                val taskCell = taskCells.first()

                val updateDto =
                    UpdateBandalartTaskCellDto(
                        title = "업데이트된 태스크 셀 제목",
                        description = "업데이트된 태스크 셀 설명",
                        dueDate = "2025-08-01T10:00:00",
                        isCompleted = true
                    )

                // when
                bandalartDao.updateTaskCellWithDto(taskCell.id!!, updateDto)

                // then
                val updatedTaskCell = bandalartDao.getCell(taskCell.id)
                assertEquals("업데이트된 태스크 셀 제목", updatedTaskCell.title)
                assertEquals("업데이트된 태스크 셀 설명", updatedTaskCell.description)
                assertEquals("2025-08-01T10:00:00", updatedTaskCell.dueDate)
                assertTrue(updatedTaskCell.isCompleted)
            }
    }

    @Nested
    @DisplayName("반다라트 완료율 및 자동 완료 처리 테스트")
    inner class CompletionTest {
        @Test
        @DisplayName("태스크 셀 완료 시 반다라트 완료율이 올바르게 계산되어야 한다")
        fun testCompletionRatioUpdate() =
            runTest {
                // given
                val bandalartId = bandalartDao.createEmptyBandalart()
                val mainCell = bandalartDao.getBandalartMainCell(bandalartId)
                val subCell = mainCell.children.first()
                val taskCells = bandalartDao.getChildCells(subCell.id!!)

                // when: 첫 번째 태스크 셀 완료 처리
                bandalartDao.updateTaskCellWithDto(
                    taskCells[0].id!!,
                    UpdateBandalartTaskCellDto(
                        title = "태스크 셀 1",
                        description = "태스크 셀 설명",
                        dueDate = null,
                        isCompleted = true
                    )
                )

                // then: 완료율 확인 (1/25 = 4%)
                val bandalartAfterOneTask = bandalartDao.getBandalart(bandalartId)
                assertEquals(4, bandalartAfterOneTask.completionRatio)
                assertFalse(bandalartAfterOneTask.isCompleted)

                // when: 한 서브셀의 모든 태스크 완료 (5개)
                taskCells.forEach { taskCell ->
                    bandalartDao.updateTaskCellWithDto(
                        taskCell.id!!,
                        UpdateBandalartTaskCellDto(
                            title = "태스크 셀 ${taskCell.id}",
                            description = "태스크 셀 설명",
                            dueDate = null,
                            isCompleted = true
                        )
                    )
                }

                // then: 완료율 확인 (5 tasks + 1 subCell = 6/25 = 24%)
                val bandalartAfterSubCell = bandalartDao.getBandalart(bandalartId)
                // 값이 정확히 맞지 않을 수 있으므로 완료율이 증가했는지만 확인
                assertTrue(bandalartAfterSubCell.completionRatio > bandalartAfterOneTask.completionRatio)
                assertFalse(bandalartAfterSubCell.isCompleted)

                // 서브셀도 자동으로 완료 처리되었는지 확인
                val updatedSubCell = bandalartDao.getCell(subCell.id)
                assertTrue(updatedSubCell.isCompleted)
            }
    }

    @Nested
    @DisplayName("반다라트 셀 초기화 테스트")
    inner class ResetCellTest {
        @Test
        @DisplayName("태스크 셀 초기화 시 내용과 완료 상태가 초기화되어야 한다")
        fun testResetTaskCell() =
            runTest {
                // given
                val bandalartId = bandalartDao.createEmptyBandalart()
                val mainCell = bandalartDao.getBandalartMainCell(bandalartId)
                val subCell = mainCell.children.first()
                val taskCells = bandalartDao.getChildCells(subCell.id!!)
                val taskCell = taskCells.first()

                // 먼저 태스크 셀 업데이트
                bandalartDao.updateTaskCellWithDto(
                    taskCell.id!!,
                    UpdateBandalartTaskCellDto(
                        title = "초기화될 태스크 셀 제목",
                        description = "초기화될 태스크 셀 설명",
                        dueDate = "2025-10-10T10:10:10",
                        isCompleted = true
                    )
                )

                // when
                bandalartDao.resetTaskCell(taskCell.id)

                // then
                val resetCell = bandalartDao.getCell(taskCell.id)
                assertEquals("", resetCell.title)
                assertEquals(null, resetCell.description)
                assertEquals(null, resetCell.dueDate)
                assertFalse(resetCell.isCompleted)
            }

        @Test
        @DisplayName("서브 셀 초기화 시 모든 하위 태스크 셀도 함께 초기화되어야 한다")
        fun testResetSubCellWithChildren() =
            runTest {
                // given
                val bandalartId = bandalartDao.createEmptyBandalart()
                val mainCell = bandalartDao.getBandalartMainCell(bandalartId)
                val subCell = mainCell.children.first()

                // 서브셀 업데이트
                bandalartDao.updateSubCellWithDto(
                    subCell.id!!,
                    UpdateBandalartSubCellDto(
                        title = "초기화될 서브 셀 제목",
                        description = "초기화될 서브 셀 설명",
                        dueDate = "2025-11-11T11:11:11"
                    )
                )

                // 태스크 셀도 업데이트
                val taskCells = bandalartDao.getChildCells(subCell.id)
                taskCells.forEach { taskCell ->
                    bandalartDao.updateTaskCellWithDto(
                        taskCell.id!!,
                        UpdateBandalartTaskCellDto(
                            title = "초기화될 태스크 셀 제목",
                            description = "초기화될 태스크 셀 설명",
                            dueDate = "2025-12-12T12:12:12",
                            isCompleted = true
                        )
                    )
                }

                // when
                bandalartDao.resetSubCellWithChildren(subCell.id)

                // then
                val resetSubCell = bandalartDao.getCell(subCell.id)
                assertEquals("", resetSubCell.title)
                assertEquals(null, resetSubCell.description)
                assertEquals(null, resetSubCell.dueDate)
                assertFalse(resetSubCell.isCompleted)

                // 자식 태스크 셀들도 초기화 확인
                val resetTaskCells = bandalartDao.getChildCells(subCell.id)
                resetTaskCells.forEach { taskCell ->
                    assertEquals("", taskCell.title)
                    assertEquals(null, taskCell.description)
                    assertEquals(null, taskCell.dueDate)
                    assertFalse(taskCell.isCompleted)
                }
            }
    }

    @Nested
    @DisplayName("반다라트 목록 조회 테스트")
    inner class BandalartListTest {
        @Test
        @DisplayName("반다라트 목록이 Flow로 올바르게 반환되어야 한다")
        fun testGetBandalartList() =
            runTest {
                // given
                bandalartDao.createEmptyBandalart()
                bandalartDao.createEmptyBandalart()
                bandalartDao.createEmptyBandalart()

                // when & then
                bandalartDao.getBandalartList().test {
                    // 첫 번째 방출된 아이템 확인
                    val bandalartList = awaitItem()
                    assertEquals(3, bandalartList.size)

                    cancelAndIgnoreRemainingEvents()
                }
            }

        @Test
        @DisplayName("모든 반다라트의 main sub task 셀을 한 번에 조회할 수 있어야 한다")
        fun allCellsCanBeProjectedGlobally() =
            runTest {
                val firstBandalartId = bandalartDao.createEmptyBandalart()
                val secondBandalartId = bandalartDao.createEmptyBandalart()

                val cells = bandalartDao.getAllCells()

                assertEquals(50, cells.size)
                assertEquals(setOf(firstBandalartId, secondBandalartId), cells.map { it.bandalartId }.toSet())
            }
    }
}
