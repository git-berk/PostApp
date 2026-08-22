package com.berco.postapp.data.repository

import com.berco.postapp.data.remote.PostApi
import com.berco.postapp.data.remote.dto.PostDto
import com.berco.postapp.domain.repository.SyncState
import com.berco.postapp.domain.result.AppError
import com.berco.postapp.domain.util.DispatcherProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class PostRepositoryImplTest {

    private val api = FakePostApi()

    private fun TestDispatchers(dispatcher: CoroutineDispatcher) = object : DispatcherProvider {
        override val io = dispatcher
        override val default = dispatcher
        override val main = dispatcher
    }

    private fun runRepositoryTest(
        body: suspend kotlinx.coroutines.test.TestScope.(PostRepositoryImpl) -> Unit,
    ) = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        body(PostRepositoryImpl(api, TestDispatchers(dispatcher)))
    }

    @Test
    fun `starts NotLoaded with no posts`() = runRepositoryTest { repository ->
        assertEquals(SyncState.NotLoaded, repository.syncState.value)
        assertEquals(emptyList<Any>(), repository.posts.first())
    }

    @Test
    fun `a successful refresh publishes posts and becomes Loaded`() = runRepositoryTest { repo ->
        api.returns(listOf(dto(1), dto(2)))

        repo.refresh()

        assertEquals(SyncState.Loaded, repo.syncState.value)
        assertEquals(listOf(1, 2), repo.posts.first().map { it.id })
    }

    @Test
    fun `a network failure maps to AppError Network`() = runRepositoryTest { repo ->
        api.throws(IOException("offline"))

        repo.refresh()

        assertEquals(SyncState.Failed(AppError.Network), repo.syncState.value)
    }

    @Test
    fun `an http failure carries the status code`() = runRepositoryTest { repo ->
        api.throws(httpException(503))

        repo.refresh()

        assertEquals(SyncState.Failed(AppError.Server(503)), repo.syncState.value)
    }

    // A failed refresh must not discard posts that were already fetched.
    @Test
    fun `a failed refresh keeps the previously loaded posts`() = runRepositoryTest { repo ->
        api.returns(listOf(dto(1)))
        repo.refresh()

        api.throws(IOException("offline"))
        repo.refresh()

        assertEquals(listOf(1), repo.posts.first().map { it.id })
        assertEquals(SyncState.Failed(AppError.Network), repo.syncState.value)
    }

    // Queueing would make three taps of Retry cost three round trips.
    @Test
    fun `a refresh that arrives while one is running is dropped, not queued`() =
        runRepositoryTest { repo ->
            val gate = CompletableDeferred<Unit>()
            api.suspendsOn(gate, listOf(dto(1)))

            val first = launch { repo.refresh() }
            runCurrent()
            val second = launch { repo.refresh() }
            runCurrent()

            gate.complete(Unit)
            first.join()
            second.join()
            advanceUntilIdle()

            assertEquals(1, api.callCount)
            assertEquals(SyncState.Loaded, repo.syncState.value)
        }

    // The repository outlives the ViewModel that cancelled it, so a stranded
    // Loading would stop the next ViewModel from ever refreshing.
    @Test
    fun `cancelling a refresh restores the previous status instead of reporting failure`() =
        runRepositoryTest { repo ->
            val gate = CompletableDeferred<Unit>()
            api.suspendsOn(gate, emptyList())

            val job = launch { repo.refresh() }
            runCurrent()
            assertEquals(SyncState.Loading, repo.syncState.value)

            job.cancelAndJoin()

            assertEquals(SyncState.NotLoaded, repo.syncState.value)
        }

    @Test
    fun `a refresh can run again after a cancelled one released the lock`() =
        runRepositoryTest { repo ->
            val gate = CompletableDeferred<Unit>()
            api.suspendsOn(gate, emptyList())
            val job = launch { repo.refresh() }
            runCurrent()
            job.cancelAndJoin()

            api.returns(listOf(dto(7)))
            repo.refresh()

            assertEquals(SyncState.Loaded, repo.syncState.value)
            assertEquals(listOf(7), repo.posts.first().map { it.id })
        }

    // ── mutations ─────────────────────────────────────────────────────────────

    @Test
    fun `updateTitle edits the post and reports the change`() = runRepositoryTest { repo ->
        api.returns(listOf(dto(1), dto(2)))
        repo.refresh()

        assertTrue(repo.updateTitle(id = 2, title = "edited"))
        assertEquals("edited", repo.posts.first().first { it.id == 2 }.title)
    }

    @Test
    fun `updateTitle reports no change for an identical value`() = runRepositoryTest { repo ->
        api.returns(listOf(dto(1, title = "Same")))
        repo.refresh()

        assertFalse(repo.updateTitle(id = 1, title = "Same"))
    }

    @Test
    fun `delete removes the post and reports the change`() = runRepositoryTest { repo ->
        api.returns(listOf(dto(1), dto(2)))
        repo.refresh()

        assertTrue(repo.delete(id = 1))
        assertEquals(listOf(2), repo.posts.first().map { it.id })
        assertFalse(repo.delete(id = 99))
    }

    private fun dto(id: Int, title: String = "Title $id", body: String = "Body $id") =
        PostDto(id = id, userId = 1, title = title, body = body)

    private fun httpException(code: Int) = HttpException(
        Response.error<List<PostDto>>(code, "".toResponseBody("application/json".toMediaType())),
    )
}

private class FakePostApi : PostApi {

    var callCount = 0
        private set

    private var behaviour: suspend () -> List<PostDto> = { emptyList() }

    fun returns(posts: List<PostDto>) {
        behaviour = { posts }
    }

    fun throws(error: Throwable) {
        behaviour = { throw error }
    }

    fun suspendsOn(gate: CompletableDeferred<Unit>, posts: List<PostDto>) {
        behaviour = { gate.await(); posts }
    }

    override suspend fun getPosts(): List<PostDto> {
        callCount++
        return behaviour()
    }
}
