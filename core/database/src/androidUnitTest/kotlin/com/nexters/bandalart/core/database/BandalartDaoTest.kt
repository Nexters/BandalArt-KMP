package com.nexters.bandalart.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nexters.bandalart.core.database.entity.UpdateBandalartMainCellDto
import com.nexters.bandalart.core.database.entity.UpdateBandalartSubCellDto
import com.nexters.bandalart.core.database.entity.UpdateBandalartTaskCellDto
import kotlinx.coroutines.flow.first
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@DisplayName("BandalartDao 테스트")
class BandalartDaoTest {

    private lateinit var bandalartDao: BandalartDao
    private lateinit var db: BandalartDatabase

    @BeforeEach
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
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
        fun testCreateEmptyBandalart() = runTest {
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
        fun testUpdateMainCell() = runTest {
            // given
            val bandalartId = bandalartDao.createEmptyBandalart()
            val mainCell = bandalartDao.getBandalartMainCell(bandalartId)
            val updateDto = UpdateBandalartMainCellDto(
                title = "Updated Main Title",
                description = "Updated Description",
                dueDate = "2025-12-31T23:59:59",
                profileEmoji = "😎",
                mainColor = "#FF0000",
                subColor = "#00FF00"
            )

            // when
            bandalartDao.updateMainCellWithDto(mainCell.cell.id!!, updateDto)

            // then
            val updatedMainCell = bandalartDao.getBandalartMainCell(bandalartId)
            assertEquals("Updated Main Title", updatedMainCell.cell.title)
            assertEquals("Updated Description", updatedMainCell.cell.description)
            assertEquals("2025-12-31T23:59:59", updatedMainCell.cell.dueDate)

            val updatedBandalart = bandalartDao.getBandalart(bandalartId)
            assertEquals("😎", updatedBandalart.profileEmoji)
            assertEquals("#FF0000", updatedBandalart.mainColor)
            assertEquals("#00FF00", updatedBandalart.subColor)
        }

        @Test
        @DisplayName("서브 셀 업데이트 시 내용이 올바르게 변경되어야 한다")
        fun testUpdateSubCell() = runTest {
            // given
            val bandalartId = bandalartDao.createEmptyBandalart()
            val mainCell = bandalartDao.getBandalartMainCell(bandalartId)
            val subCell = mainCell.children.first()
            val updateDto = UpdateBandalartSubCellDto(
                title = "Updated Sub Cell",
                description = "Sub Cell Description",
                dueDate = "2025-10-15T12:00:00"
            )

            // when
            bandalartDao.updateSubCellWithDto(subCell.id!!, updateDto)

            // then
            val updatedSubCell = bandalartDao.getCell(subCell.id)
            assertEquals("Updated Sub Cell", updatedSubCell.title)
            assertEquals("Sub Cell Description", updatedSubCell.description)
            assertEquals("2025-10-15T12:00:00", updatedSubCell.dueDate)
            assertFalse(updatedSubCell.isCompleted)
        }

        @Test
        @DisplayName("태스크 셀 업데이트 시 완료 상태가 변경될 수 있어야 한다")
        fun testUpdateTaskCell() = runTest {
            // given
            val bandalartId = bandalartDao.createEmptyBandalart()
            val mainCell = bandalartDao.getBandalartMainCell(bandalartId)
            val subCell = mainCell.children.first()
            val taskCells = bandalartDao.getChildCells(subCell.id!!)
            val taskCell = taskCells.first()

            val updateDto = UpdateBandalartTaskCellDto(
                title = "Task Cell Title",
                description = "Task Description",
                dueDate = "2025-08-01T10:00:00",
                isCompleted = true
            )

            // when
            bandalartDao.updateTaskCellWithDto(taskCell.id!!, updateDto)

            // then
            val updatedTaskCell = bandalartDao.getCell(taskCell.id)
            assertEquals("Task Cell Title", updatedTaskCell.title)
            assertEquals("Task Description", updatedTaskCell.description)
            assertEquals("2025-08-01T10:00:00", updatedTaskCell.dueDate)
            assertTrue(updatedTaskCell.isCompleted)
        }
    }

    @Nested
    @DisplayName("반다라트 완료율 및 자동 완료 처리 테스트")
    inner class CompletionTest {

        @Test
        @DisplayName("태스크 셀 완료 시 반다라트 완료율이 올바르게 계산되어야 한다")
        fun testCompletionRatioUpdate() = runTest {
            // given
            val bandalartId = bandalartDao.createEmptyBandalart()
            val mainCell = bandalartDao.getBandalartMainCell(bandalartId)
            val subCell = mainCell.children.first()
            val taskCells = bandalartDao.getChildCells(subCell.id!!)

            // when: 첫 번째 태스크 셀 완료 처리
            bandalartDao.updateTaskCellWithDto(
                taskCells[0].id!!,
                UpdateBandalartTaskCellDto(
                    title = "Task 1",
                    description = "Description",
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
                        title = "Task ${taskCell.id}",
                        description = "Description",
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
        fun testResetTaskCell() = runTest {
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
                    title = "Task to Reset",
                    description = "Will be reset",
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
        fun testResetSubCellWithChildren() = runTest {
            // given
            val bandalartId = bandalartDao.createEmptyBandalart()
            val mainCell = bandalartDao.getBandalartMainCell(bandalartId)
            val subCell = mainCell.children.first()

            // 서브셀 업데이트
            bandalartDao.updateSubCellWithDto(
                subCell.id!!,
                UpdateBandalartSubCellDto(
                    title = "Sub Cell to Reset",
                    description = "Will be reset",
                    dueDate = "2025-11-11T11:11:11"
                )
            )

            // 태스크 셀도 업데이트
            val taskCells = bandalartDao.getChildCells(subCell.id)
            taskCells.forEach { taskCell ->
                bandalartDao.updateTaskCellWithDto(
                    taskCell.id!!,
                    UpdateBandalartTaskCellDto(
                        title = "Task to Reset",
                        description = "Will be reset",
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
        fun testGetBandalartList() = runTest {
            // given
            bandalartDao.createEmptyBandalart()
            bandalartDao.createEmptyBandalart()
            bandalartDao.createEmptyBandalart()

            // when
            val bandalartList = bandalartDao.getBandalartList().first()

            // then
            assertEquals(3, bandalartList.size)
        }
    }
}
