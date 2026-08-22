package com.berco.postapp.presentation.detail

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PostDetailViewModelTest {

    private fun viewModel(postId: Int = 7) =
        PostDetailViewModel(SavedStateHandle(mapOf("postId" to postId)))

    @Test
    fun `reads the post id from the navigation argument`() {
        assertEquals(7, viewModel().postId)
    }

    @Test
    fun `starts with no editor open and an empty draft`() {
        val state = viewModel().editState.value

        assertEquals(EditorTarget.NONE, state.target)
        assertEquals("", state.draft)
        assertFalse(state.isEditingTitle)
        assertFalse(state.isEditingBody)
    }

    @Test
    fun `startEditing opens the requested editor seeded with the current value`() {
        val viewModel = viewModel()

        viewModel.startEditing(EditorTarget.TITLE, "current title")

        val state = viewModel.editState.value
        assertTrue(state.isEditingTitle)
        assertFalse(state.isEditingBody)
        assertEquals("current title", state.draft)
    }

    @Test
    fun `startEditing on the body opens only the body editor`() {
        val viewModel = viewModel()

        viewModel.startEditing(EditorTarget.BODY, "current body")

        assertTrue(viewModel.editState.value.isEditingBody)
        assertFalse(viewModel.editState.value.isEditingTitle)
    }

    // Switching editors must not carry the previous draft across.
    @Test
    fun `startEditing replaces a draft left by the other editor`() {
        val viewModel = viewModel()
        viewModel.startEditing(EditorTarget.TITLE, "title")
        viewModel.onDraftChanged("half-typed title")

        viewModel.startEditing(EditorTarget.BODY, "body")

        assertEquals("body", viewModel.editState.value.draft)
    }

    @Test
    fun `onDraftChanged updates the draft without closing the editor`() {
        val viewModel = viewModel()
        viewModel.startEditing(EditorTarget.TITLE, "before")

        viewModel.onDraftChanged("after")

        assertEquals("after", viewModel.editState.value.draft)
        assertTrue(viewModel.editState.value.isEditingTitle)
    }

    @Test
    fun `cancelEditing closes the editor and discards the draft`() {
        val viewModel = viewModel()
        viewModel.startEditing(EditorTarget.TITLE, "original")
        viewModel.onDraftChanged("discard me")

        viewModel.cancelEditing()

        assertEquals(EditorTarget.NONE, viewModel.editState.value.target)
        assertEquals("", viewModel.editState.value.draft)
    }

    @Test
    fun `commitEdit returns the draft and closes the editor`() {
        val viewModel = viewModel()
        viewModel.startEditing(EditorTarget.TITLE, "original")
        viewModel.onDraftChanged("edited")

        assertEquals("edited", viewModel.commitEdit())
        assertEquals(EditorTarget.NONE, viewModel.editState.value.target)
    }

    @Test
    fun `commitEdit trims surrounding whitespace`() {
        val viewModel = viewModel()
        viewModel.startEditing(EditorTarget.BODY, "original")
        viewModel.onDraftChanged("   edited   ")

        assertEquals("edited", viewModel.commitEdit())
    }

    // A blank draft discards the edit rather than wiping the field.
    @Test
    fun `commitEdit returns null for a blank draft but still closes the editor`() {
        val viewModel = viewModel()
        viewModel.startEditing(EditorTarget.TITLE, "original")
        viewModel.onDraftChanged("    ")

        assertNull(viewModel.commitEdit())
        assertEquals(EditorTarget.NONE, viewModel.editState.value.target)
    }

    @Test
    fun `commitEdit with no editor open returns null`() {
        assertNull(viewModel().commitEdit())
    }

    @Test
    fun `commitEdit is not repeatable`() {
        val viewModel = viewModel()
        viewModel.startEditing(EditorTarget.TITLE, "edited")

        assertEquals("edited", viewModel.commitEdit())
        assertNull(viewModel.commitEdit())
    }
}
