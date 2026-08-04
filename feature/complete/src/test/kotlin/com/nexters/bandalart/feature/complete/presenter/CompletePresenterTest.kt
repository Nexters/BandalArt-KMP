package com.nexters.bandalart.feature.complete.presenter

import android.net.Uri
import com.nexters.bandalart.core.domain.entity.BandalartCellEntity
import com.nexters.bandalart.core.domain.entity.BandalartEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartEmojiEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartMainCellEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartSubCellEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartTaskCellEntity
import com.nexters.bandalart.core.domain.repository.BandalartRepository
import com.nexters.bandalart.feature.complete.CompleteScreen
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
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

private class RecordingBandalartRepository : BandalartRepository {
    var completion: Pair<Long, Boolean>? = null
        private set

    private val completionRecorded = kotlinx.coroutines.CompletableDeferred<Unit>()

    suspend fun awaitCompletion() = completionRecorded.await()

    override suspend fun upsertBandalartId(bandalartId: Long, isCompleted: Boolean) {
        completion = bandalartId to isCompleted
        completionRecorded.complete(Unit)
    }

    override suspend fun createBandalart(): BandalartEntity? = error("Not used")
    override fun getBandalartList(): Flow<List<BandalartEntity>> = emptyFlow()
    override suspend fun getBandalart(bandalartId: Long): BandalartEntity = error("Not used")
    override suspend fun deleteBandalart(bandalartId: Long) = error("Not used")
    override suspend fun getBandalartMainCell(bandalartId: Long): BandalartCellEntity? = error("Not used")
    override suspend fun getChildCells(parentId: Long): List<BandalartCellEntity> = error("Not used")

    override suspend fun updateBandalartMainCell(
        bandalartId: Long,
        cellId: Long,
        updateBandalartMainCellEntity: UpdateBandalartMainCellEntity,
    ) = error("Not used")

    override suspend fun updateBandalartSubCell(
        bandalartId: Long,
        cellId: Long,
        updateBandalartSubCellEntity: UpdateBandalartSubCellEntity,
    ) = error("Not used")

    override suspend fun updateBandalartTaskCell(
        bandalartId: Long,
        cellId: Long,
        updateBandalartTaskCellEntity: UpdateBandalartTaskCellEntity,
    ) = error("Not used")

    override suspend fun updateBandalartEmoji(
        bandalartId: Long,
        cellId: Long,
        updateBandalartEmojiEntity: UpdateBandalartEmojiEntity,
    ) = error("Not used")

    override suspend fun deleteBandalartCell(cellId: Long) = error("Not used")
    override suspend fun setRecentBandalartId(recentBandalartId: Long) = error("Not used")
    override suspend fun getRecentBandalartId(): Long = error("Not used")
    override suspend fun getPrevBandalartList(): List<Pair<Long, Boolean>> = error("Not used")
    override suspend fun checkCompletedBandalartId(bandalartId: Long): Boolean = error("Not used")
    override suspend fun deleteCompletedBandalartId(bandalartId: Long) = error("Not used")
}
