package com.berco.postapp.presentation.posts

import app.cash.turbine.test
import com.berco.postapp.domain.result.AppError
import com.berco.postapp.domain.usecase.GetPostsUseCase
import com.berco.postapp.testing.FakePostRepository
import com.berco.postapp.testing.MainDispatcherRule
import com.berco.postapp.testing.post
import com.berco.postapp.testing.posts
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PostsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakePostRepository()

    private fun viewModel() = PostsViewModel(GetPostsUseCase(repository))

    // ── loading ───────────────────────────────────────────────────────────────

    @Test
    fun `starts in Loading before the fetch completes`() = runTest {
        repository.returns(posts(3))

        assertEquals(PostListUiState.Loading, viewModel().uiState.value)
    }

    @Test
    fun `loads posts into Content`() = runTest {
        repository.returns(posts(3))
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(PostListUiState.Content(posts(3)), viewModel.uiState.value)
    }

    @Test
    fun `an empty response becomes Empty rather than an empty Content`() = runTest {
        repository.returns(emptyList())
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(PostListUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `a failure becomes Error carrying the domain error`() = runTest {
        repository.fails(AppError.Server(code = 503))
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(PostListUiState.Error(AppError.Server(503)), viewModel.uiState.value)
    }

    @Test
    fun `load re-fetches and returns to Loading first`() = runTest {
        repository.returns(posts(2))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            assertTrue(awaitItem() is PostListUiState.Content)

            viewModel.load()
            assertEquals(PostListUiState.Loading, awaitItem())

            advanceUntilIdle()
            assertTrue(awaitItem() is PostListUiState.Content)
        }
        assertEquals(2, repository.callCount)
    }

    // Setting Loading between attempts is what makes a repeated identical failure
    // re-render; StateFlow would otherwise conflate Error(Network) with itself.
    @Test
    fun `a repeated identical failure still emits`() = runTest {
        repository.fails(AppError.Network)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            assertEquals(PostListUiState.Error(AppError.Network), awaitItem())

            viewModel.load()
            assertEquals(PostListUiState.Loading, awaitItem())

            advanceUntilIdle()
            assertEquals(PostListUiState.Error(AppError.Network), awaitItem())
        }
    }

    // ── editing ───────────────────────────────────────────────────────────────

    @Test
    fun `updateTitle changes only the target post and reports the change`() = runTest {
        repository.returns(posts(3))
        val viewModel = viewModel()
        advanceUntilIdle()

        val changed = viewModel.updateTitle(id = 2, title = "edited")

        assertTrue(changed)
        val result = (viewModel.uiState.value as PostListUiState.Content).posts
        assertEquals("edited", result[1].title)
        assertEquals(listOf("Title 1", "edited", "Title 3"), result.map { it.title })
    }

    @Test
    fun `updateTitle keeps the identity of untouched posts so DiffUtil sees one change`() =
        runTest {
            repository.returns(posts(3))
            val viewModel = viewModel()
            advanceUntilIdle()
            val before = (viewModel.uiState.value as PostListUiState.Content).posts

            viewModel.updateTitle(id = 2, title = "edited")

            val after = (viewModel.uiState.value as PostListUiState.Content).posts
            assertSame(before[0], after[0])
            assertNotSame(before[1], after[1])
            assertSame(before[2], after[2])
        }

    @Test
    fun `updateBody changes only the body`() = runTest {
        repository.returns(posts(2))
        val viewModel = viewModel()
        advanceUntilIdle()

        assertTrue(viewModel.updateBody(id = 1, body = "edited"))

        val post = (viewModel.uiState.value as PostListUiState.Content).posts[0]
        assertEquals("edited", post.body)
        assertEquals("Title 1", post.title)
    }

    // Saving an untouched field must not claim a save; the detail screen keys its
    // confirmation pill off this return value.
    @Test
    fun `updateTitle reports no change when the value is identical`() = runTest {
        repository.returns(posts(2))
        val viewModel = viewModel()
        advanceUntilIdle()
        val before = viewModel.uiState.value

        val changed = viewModel.updateTitle(id = 1, title = "Title 1")

        assertFalse(changed)
        assertSame(before, viewModel.uiState.value)
    }

    @Test
    fun `updateTitle reports no change for an unknown id`() = runTest {
        repository.returns(posts(2))
        val viewModel = viewModel()
        advanceUntilIdle()

        assertFalse(viewModel.updateTitle(id = 99, title = "edited"))
    }

    @Test
    fun `edits are ignored while not in Content`() = runTest {
        repository.fails()
        val viewModel = viewModel()
        advanceUntilIdle()

        assertFalse(viewModel.updateTitle(id = 1, title = "edited"))
        assertEquals(PostListUiState.Error(AppError.Network), viewModel.uiState.value)
    }

    @Test
    fun `an identical edit produces no emission`() = runTest {
        repository.returns(posts(2))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            awaitItem()
            viewModel.updateTitle(id = 1, title = "Title 1")
            expectNoEvents()
        }
    }

    // ── deleting ──────────────────────────────────────────────────────────────

    @Test
    fun `delete removes the post and reports the change`() = runTest {
        repository.returns(posts(3))
        val viewModel = viewModel()
        advanceUntilIdle()

        assertTrue(viewModel.delete(id = 2))

        val result = (viewModel.uiState.value as PostListUiState.Content).posts
        assertEquals(listOf(1, 3), result.map { it.id })
    }

    @Test
    fun `deleting the last remaining post moves to Empty`() = runTest {
        repository.returns(listOf(post(1)))
        val viewModel = viewModel()
        advanceUntilIdle()

        assertTrue(viewModel.delete(id = 1))
        assertEquals(PostListUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `delete reports no change for an unknown id`() = runTest {
        repository.returns(posts(2))
        val viewModel = viewModel()
        advanceUntilIdle()

        assertFalse(viewModel.delete(id = 99))
    }

    // ── single post lookup ────────────────────────────────────────────────────

    @Test
    fun `currentPost returns the post or null`() = runTest {
        repository.returns(posts(2))
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals("Title 2", viewModel.currentPost(2)?.title)
        assertNull(viewModel.currentPost(99))
    }

    @Test
    fun `currentPost is null before the list has loaded`() = runTest {
        repository.returns(posts(2))

        assertNull(viewModel().currentPost(1))
    }

    @Test
    fun `post emits the post and then the edited post`() = runTest {
        repository.returns(posts(3))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.post(2).test {
            assertEquals("Title 2", awaitItem()?.title)

            viewModel.updateTitle(id = 2, title = "edited")
            assertEquals("edited", awaitItem()?.title)
        }
    }

    // Without distinctUntilChanged the detail screen would re-render on every
    // unrelated change to the list.
    @Test
    fun `post does not re-emit when a different post changes`() = runTest {
        repository.returns(posts(3))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.post(2).test {
            assertEquals("Title 2", awaitItem()?.title)

            viewModel.updateTitle(id = 3, title = "edited")
            expectNoEvents()
        }
    }

    @Test
    fun `post emits null once its post is deleted`() = runTest {
        repository.returns(posts(3))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.post(2).test {
            assertEquals("Title 2", awaitItem()?.title)

            viewModel.delete(id = 2)
            assertNull(awaitItem())
        }
    }
}
