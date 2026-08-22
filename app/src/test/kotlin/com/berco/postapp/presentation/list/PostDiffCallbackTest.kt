package com.berco.postapp.presentation.list

import com.berco.postapp.testing.post
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PostDiffCallbackTest {

    @Test
    fun `posts are the same item when the id matches`() {
        val old = post(1, title = "before")
        val new = post(1, title = "after")

        assertTrue(PostDiffCallback.areItemsTheSame(old, new))
    }

    @Test
    fun `posts are different items when the id differs`() {
        assertFalse(PostDiffCallback.areItemsTheSame(post(1), post(2)))
    }

    @Test
    fun `contents are the same for an identical post`() {
        assertTrue(PostDiffCallback.areContentsTheSame(post(1), post(1)))
    }

    @Test
    fun `contents differ when the title changes`() {
        assertFalse(PostDiffCallback.areContentsTheSame(post(1), post(1, title = "edited")))
    }

    // The payload is what keeps an edit to one TextView instead of a full rebind
    // that would re-request the row's image.
    @Test
    fun `a title edit yields a title-only payload`() {
        val payload = PostDiffCallback.getChangePayload(post(1), post(1, title = "edited"))

        assertEquals(setOf(PostField.TITLE), payload)
    }

    @Test
    fun `a body edit yields a body-only payload`() {
        val payload = PostDiffCallback.getChangePayload(post(1), post(1, body = "edited"))

        assertEquals(setOf(PostField.BODY), payload)
    }

    @Test
    fun `editing both fields yields both in the payload`() {
        val payload = PostDiffCallback.getChangePayload(
            post(1),
            post(1, title = "edited", body = "edited"),
        )

        assertEquals(setOf(PostField.TITLE, PostField.BODY), payload)
    }

    // A null payload makes RecyclerView fall back to a full bind, which is the
    // correct outcome when the change is in a field the payload cannot describe.
    @Test
    fun `a change the payload cannot describe returns null`() {
        val payload = PostDiffCallback.getChangePayload(post(1), post(1, imageIndex = 42))

        assertNull(payload)
    }

    @Test
    fun `identical posts return no payload`() {
        assertNull(PostDiffCallback.getChangePayload(post(1), post(1)))
    }
}
