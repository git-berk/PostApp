package com.berco.postapp.presentation.detail

import androidx.lifecycle.SavedStateHandle
import com.berco.postapp.testing.FakePostRepository
import com.berco.postapp.testing.MainDispatcherRule
import com.berco.postapp.testing.posts
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PostDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakePostRepository()

    // uiState is shared WhileSubscribed, so it stays at its initial value until
    // something collects it and the sharing coroutine has been allowed to run.
    private fun TestScope.viewModel(postId: Int = 2) =
        PostDetailViewModel(repository, SavedStateHandle(mapOf("postId" to postId))).also { vm ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { } }
            testScheduler.advanceUntilIdle()
        }

    private fun PostDetailViewModel.content() = uiState.value as PostDetailUiState.Content

    @Test
    fun `reads the post id from the navigation argument`() {
        val viewModel = PostDetailViewModel(repository, SavedStateHandle(mapOf("postId" to 2)))

        assertEquals(2, viewModel.postId)
    }

    // Navigation restores this screen before the data layer has anything in it.
    // Treating the missing post as Deleted here would bounce the user straight out
    // of the screen they were restored onto.
    @Test
    fun `shows Loading while the fetch it started is still in flight`() = runTest {
        repository.refreshNeverCompletes()
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(PostDetailUiState.Loading, viewModel.uiState.value)
    }

    // Restored straight onto this screen, the list fragment is only CREATED and its
    // ViewModel is never constructed — so this screen must load the data itself or
    // it would sit on Loading forever.
    @Test
    fun `loads the data itself when nothing has been fetched yet`() = runTest {
        repository.refreshReturns(posts(3))

        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(1, repository.refreshCount)
        assertEquals(2, viewModel.content().post.id)
    }

    @Test
    fun `does not re-fetch when the repository has already loaded`() = runTest {
        repository.setLoaded(posts(3))

        viewModel()
        advanceUntilIdle()

        assertEquals(0, repository.refreshCount)
    }

    // Only a post genuinely absent from a loaded list is Deleted.
    @Test
    fun `becomes Deleted when the post is removed from a loaded list`() = runTest {
        repository.setLoaded(posts(3))
        val viewModel = viewModel()
        assertTrue(viewModel.uiState.value is PostDetailUiState.Content)

        repository.delete(2)
        advanceUntilIdle()

        assertEquals(PostDetailUiState.Deleted, viewModel.uiState.value)
    }

    @Test
    fun `is Deleted when the loaded list never contained the post`() = runTest {
        repository.setLoaded(posts(1))

        assertEquals(PostDetailUiState.Deleted, viewModel(postId = 99).uiState.value)
    }

    @Test
    fun `starts with no editor open`() = runTest {
        repository.setLoaded(posts(3))

        assertEquals(EditorTarget.NONE, viewModel().content().edit.target)
    }

    @Test
    fun `startEditing opens the editor seeded with the current value`() = runTest {
        repository.setLoaded(posts(3))
        val viewModel = viewModel()

        viewModel.startEditing(EditorTarget.TITLE, "Title 2")
        advanceUntilIdle()

        val edit = viewModel.content().edit
        assertTrue(edit.isEditingTitle)
        assertFalse(edit.isEditingBody)
        assertEquals("Title 2", edit.draft)
    }

    @Test
    fun `switching editors replaces the previous draft`() = runTest {
        repository.setLoaded(posts(3))
        val viewModel = viewModel()
        viewModel.startEditing(EditorTarget.TITLE, "Title 2")
        viewModel.onDraftChanged("half typed")

        viewModel.startEditing(EditorTarget.BODY, "Body 2")
        advanceUntilIdle()

        val edit = viewModel.content().edit
        assertTrue(edit.isEditingBody)
        assertEquals("Body 2", edit.draft)
    }

    @Test
    fun `onDraftChanged updates the draft without closing the editor`() = runTest {
        repository.setLoaded(posts(3))
        val viewModel = viewModel()
        viewModel.startEditing(EditorTarget.TITLE, "before")

        viewModel.onDraftChanged("after")
        advanceUntilIdle()

        val edit = viewModel.content().edit
        assertEquals("after", edit.draft)
        assertTrue(edit.isEditingTitle)
    }

    @Test
    fun `cancelEditing closes the editor and discards the draft`() = runTest {
        repository.setLoaded(posts(3))
        val viewModel = viewModel()
        viewModel.startEditing(EditorTarget.TITLE, "Title 2")
        viewModel.onDraftChanged("discard me")

        viewModel.cancelEditing()
        advanceUntilIdle()

        val edit = viewModel.content().edit
        assertEquals(EditorTarget.NONE, edit.target)
        assertEquals("", edit.draft)
        assertEquals("Title 2", viewModel.content().post.title)
    }

    @Test
    fun `commitEdit writes a title edit through to the repository`() = runTest {
        repository.setLoaded(posts(3))
        val viewModel = viewModel()
        viewModel.startEditing(EditorTarget.TITLE, "Title 2")
        viewModel.onDraftChanged("edited")

        assertTrue(viewModel.commitEdit())
        advanceUntilIdle()
        assertEquals("edited", viewModel.content().post.title)
    }

    // The ViewModel routes by the open editor, so the fragment does not have to
    // remember which field its Save button belongs to.
    @Test
    fun `commitEdit routes a body edit to the body`() = runTest {
        repository.setLoaded(posts(3))
        val viewModel = viewModel()
        viewModel.startEditing(EditorTarget.BODY, "Body 2")
        viewModel.onDraftChanged("edited")

        viewModel.commitEdit()
        advanceUntilIdle()

        assertEquals("edited", viewModel.content().post.body)
        assertEquals("Title 2", viewModel.content().post.title)
    }

    @Test
    fun `commitEdit trims the draft`() = runTest {
        repository.setLoaded(posts(3))
        val viewModel = viewModel()
        viewModel.startEditing(EditorTarget.TITLE, "Title 2")
        viewModel.onDraftChanged("   edited   ")

        viewModel.commitEdit()
        advanceUntilIdle()

        assertEquals("edited", viewModel.content().post.title)
    }

    // Saving an untouched field must not claim a save — the confirmation pill is
    // driven by this flag.
    @Test
    fun `commitEdit reports no change when the value is identical`() = runTest {
        repository.setLoaded(posts(3))
        val viewModel = viewModel()
        viewModel.startEditing(EditorTarget.TITLE, "Title 2")

        assertFalse(viewModel.commitEdit())
    }

    @Test
    fun `a blank draft discards the edit and never reports a save`() = runTest {
        repository.setLoaded(posts(3))
        val viewModel = viewModel()
        viewModel.startEditing(EditorTarget.TITLE, "Title 2")
        viewModel.onDraftChanged("    ")

        assertFalse(viewModel.commitEdit())
        advanceUntilIdle()
        assertEquals("Title 2", viewModel.content().post.title)
        assertEquals(EditorTarget.NONE, viewModel.content().edit.target)
    }

    @Test
    fun `commitEdit closes the editor`() = runTest {
        repository.setLoaded(posts(3))
        val viewModel = viewModel()
        viewModel.startEditing(EditorTarget.TITLE, "edited")

        viewModel.commitEdit()
        advanceUntilIdle()

        assertEquals(EditorTarget.NONE, viewModel.content().edit.target)
    }
}
