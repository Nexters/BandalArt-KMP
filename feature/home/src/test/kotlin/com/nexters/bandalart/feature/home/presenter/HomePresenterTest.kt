package com.nexters.bandalart.feature.home.presenter

import com.nexters.bandalart.core.domain.entity.BandalartCellEntity
import com.nexters.bandalart.core.domain.entity.BandalartEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartEmojiEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartMainCellEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartSubCellEntity
import com.nexters.bandalart.core.domain.entity.UpdateBandalartTaskCellEntity
import com.nexters.bandalart.core.domain.repository.BandalartRepository
import com.nexters.bandalart.core.domain.repository.InAppUpdateRepository
import com.nexters.bandalart.feature.home.HomeScreen
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

private class FakeInAppUpdateRepository : InAppUpdateRepository {
    var rejectedVersionCode: Int? = null
        private set

    override suspend fun setLastRejectedUpdateVersion(rejectedVersionCode: Int) {
        this.rejectedVersionCode = rejectedVersionCode
    }

    override suspend fun isUpdateAlreadyRejected(updateVersionCode: Int): Boolean {
        return rejectedVersionCode == updateVersionCode
    }
}

private class FakeBandalartRepository(
    bandalarts: List<BandalartEntity>,
    var recentBandalartId: Long = bandalarts.firstOrNull()?.id ?: 0L,
    private val createdBandalart: BandalartEntity? = null,
) : BandalartRepository {
    private val bandalartFlow = MutableStateFlow(bandalarts)
    private val createCalled = CompletableDeferred<Unit>()

    var createCalls = 0
        private set

    suspend fun awaitCreate() = createCalled.await()

    override suspend fun createBandalart(): BandalartEntity? {
        createCalls += 1
        createCalled.complete(Unit)
        return createdBandalart
    }

    override fun getBandalartList(): Flow<List<BandalartEntity>> = bandalartFlow

    override suspend fun getBandalart(bandalartId: Long): BandalartEntity {
        return bandalartFlow.value.find { it.id == bandalartId }
            ?: createdBandalart?.takeIf { it.id == bandalartId }
            ?: error("Unknown bandalart: $bandalartId")
    }

    override suspend fun getBandalartMainCell(bandalartId: Long): BandalartCellEntity {
        return BandalartCellEntity(id = bandalartId, bandalartId = bandalartId, parentId = null)
    }

    override suspend fun getChildCells(parentId: Long): List<BandalartCellEntity> = emptyList()

    override suspend fun setRecentBandalartId(recentBandalartId: Long) {
        this.recentBandalartId = recentBandalartId
    }

    override suspend fun getRecentBandalartId(): Long = recentBandalartId

    override suspend fun getPrevBandalartList(): List<Pair<Long, Boolean>> {
        return bandalartFlow.value.map { it.id to it.isCompleted }
    }

    override suspend fun upsertBandalartId(bandalartId: Long, isCompleted: Boolean) = Unit
    override suspend fun deleteBandalart(bandalartId: Long) = error("Not used")

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
    override suspend fun checkCompletedBandalartId(bandalartId: Long): Boolean = false
    override suspend fun deleteCompletedBandalartId(bandalartId: Long) = error("Not used")
}

private fun bandalart(id: Long) = BandalartEntity(
    id = id,
    mainColor = "#3FFFBA",
    subColor = "#111827",
    profileEmoji = "🚀",
    title = "반다라트 $id",
    description = null,
    dueDate = null,
    isCompleted = false,
    completionRatio = 0,
)
