package com.berco.postapp.presentation.posts

import app.cash.turbine.test
import com.berco.postapp.domain.repository.SyncState
import com.berco.postapp.domain.result.AppError
import com.berco.postapp.testing.FakePostRepository
import com.berco.postapp.testing.MainDispatcherRule
import com.berco.postapp.testing.post
import com.berco.postapp.testing.posts
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PostsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakePostRepository()

    // uiState is shared WhileSubscribed, so it stays at its initial value until
    // something collects. Every test keeps a collector alive for the test's lifetime.
    private fun TestScope.viewModel() = PostsViewModel(repository).also { vm ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { } }
    }

    // ── the empty-list ambiguity ──────────────────────────────────────────────

    // Both of the next two tests see posts == emptyList(); only syncState tells
    // them apart. Getting this wrong flashes "you deleted every post" on launch.
    @Test
    fun `an empty repository that has never loaded is Loading, not Empty`() = runTest {
        assertEquals(PostListUiState.Loading, toPostListUiState(emptyList(), SyncState.NotLoaded))
    }

    @Test
    fun `an empty repository that has loaded is Empty`() = runTest {
        assertEquals(PostListUiState.Empty, toPostListUiState(emptyList(), SyncState.Loaded))
    }

    @Test
    fun `a failed load with nothing cached is a full-screen Error`() = runTest {
        assertEquals(
            PostListUiState.Error(AppError.Network),
            toPostListUiState(emptyList(), SyncState.Failed(AppError.Network)),
        )
    }

    // A failed refresh must not throw away posts that are already on screen.
    @Test
    fun `a failed refresh with posts cached stays on Content and reports the error`() = runTest {
        val state = toPostListUiState(posts(2), SyncState.Failed(AppError.Network))

        assertEquals(PostListUiState.Content(posts(2), AppError.Network), state)
    }

    // ── loading ───────────────────────────────────────────────────────────────

    @Test
    fun `refreshes on a cold start and publishes the posts`() = runTest {
        repository.refreshReturns(posts(3))
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(PostListUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(PostListUiState.Content(posts(3)), awaitItem())
        }
        assertEquals(1, repository.refreshCount)
    }

    // Recreating the ViewModel after the data layer has loaded must not re-fetch,
    // or every return to the list would wipe the user's local edits and deletes.
    @Test
    fun `does not refresh when the repository has already loaded`() = runTest {
        repository.setLoaded(posts(3))

        viewModel()
        advanceUntilIdle()

        assertEquals(0, repository.refreshCount)
    }

    @Test
    fun `an empty response becomes Empty`() = runTest {
        repository.refreshReturns(emptyList())
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(PostListUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `a failure becomes Error carrying the domain error`() = runTest {
        repository.refreshFails(AppError.Server(code = 503))
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(PostListUiState.Error(AppError.Server(503)), viewModel.uiState.value)
    }

    @Test
    fun `refresh re-fetches on demand`() = runTest {
        repository.refreshFails()
        val viewModel = viewModel()
        advanceUntilIdle()

        repository.refreshReturns(posts(2))
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(PostListUiState.Content(posts(2)), viewModel.uiState.value)
        assertEquals(2, repository.refreshCount)
    }

    // ── editing ───────────────────────────────────────────────────────────────

    @Test
    fun `updateTitle changes only the target post`() = runTest {
        repository.setLoaded(posts(3))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.updateTitle(id = 2, title = "edited")
        advanceUntilIdle()

        val result = (viewModel.uiState.value as PostListUiState.Content).posts
        assertEquals(listOf("Title 1", "edited", "Title 3"), result.map { it.title })
    }

    @Test
    fun `updateTitle keeps the identity of untouched posts so DiffUtil sees one change`() =
        runTest {
            repository.setLoaded(posts(3))
            val viewModel = viewModel()
            advanceUntilIdle()
            val before = (viewModel.uiState.value as PostListUiState.Content).posts

            viewModel.updateTitle(id = 2, title = "edited")
            advanceUntilIdle()

            val after = (viewModel.uiState.value as PostListUiState.Content).posts
            assertSame(before[0], after[0])
            assertNotSame(before[1], after[1])
            assertSame(before[2], after[2])
        }

    @Test
    fun `updateBody changes only the body`() = runTest {
        repository.setLoaded(posts(2))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.updateBody(id = 1, body = "edited")
        advanceUntilIdle()

        val post = (viewModel.uiState.value as PostListUiState.Content).posts[0]
        assertEquals("edited", post.body)
        assertEquals("Title 1", post.title)
    }

    @Test
    fun `updateTitle reports whether anything changed`() = runTest {
        repository.setLoaded(posts(2))
        val viewModel = viewModel()
        advanceUntilIdle()

        assertFalse(viewModel.updateTitle(id = 1, title = "Title 1"))
        assertTrue(viewModel.updateTitle(id = 1, title = "edited"))
    }

    @Test
    fun `an identical edit produces no emission`() = runTest {
        repository.setLoaded(posts(2))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            awaitItem()
            viewModel.updateTitle(id = 1, title = "Title 1")
            advanceUntilIdle()
            expectNoEvents()
        }
    }

    // ── deleting ──────────────────────────────────────────────────────────────

    @Test
    fun `delete removes the post`() = runTest {
        repository.setLoaded(posts(3))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.delete(id = 2)
        advanceUntilIdle()

        val result = (viewModel.uiState.value as PostListUiState.Content).posts
        assertEquals(listOf(1, 3), result.map { it.id })
    }

    // The scenario the empty-list ambiguity exists for: deleting everything is
    // Empty, not Loading, because the repository has already loaded.
    @Test
    fun `deleting the last remaining post moves to Empty`() = runTest {
        repository.setLoaded(listOf(post(1)))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.delete(id = 1)
        advanceUntilIdle()

        assertEquals(PostListUiState.Empty, viewModel.uiState.value)
    }
}
