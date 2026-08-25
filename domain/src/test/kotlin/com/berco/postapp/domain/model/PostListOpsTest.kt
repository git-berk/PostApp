package com.berco.postapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class PostListOpsTest {

    private val posts = listOf(post(1), post(2), post(3))

    @Test
    fun `withUpdatedTitle replaces the title of the matching post`() {
        val result = posts.withUpdatedTitle(id = 2, title = "changed")

        assertEquals("changed", result.findById(2)?.title)
    }

    @Test
    fun `withUpdatedTitle leaves every other post untouched by identity`() {
        val result = posts.withUpdatedTitle(id = 2, title = "changed")

        assertSame(posts[0], result[0])
        assertSame(posts[2], result[2])
        assertNotSame(posts[1], result[1])
    }

    @Test
    fun `withUpdatedTitle leaves the body alone`() {
        val result = posts.withUpdatedTitle(id = 2, title = "changed")

        assertEquals(posts[1].body, result[1].body)
    }

    // The identity contract callers rely on to detect a no-op without comparing
    // the whole list.
    @Test
    fun `withUpdatedTitle returns the same list instance when the title is unchanged`() {
        val result = posts.withUpdatedTitle(id = 2, title = posts[1].title)

        assertSame(posts, result)
    }

    @Test
    fun `withUpdatedTitle returns the same list instance when the id is missing`() {
        val result = posts.withUpdatedTitle(id = 99, title = "changed")

        assertSame(posts, result)
    }

    @Test
    fun `withUpdatedBody replaces the body of the matching post`() {
        val result = posts.withUpdatedBody(id = 3, body = "changed")

        assertEquals("changed", result.findById(3)?.body)
        assertEquals(posts[2].title, result[2].title)
    }

    @Test
    fun `withUpdatedBody returns the same list instance when the body is unchanged`() {
        assertSame(posts, posts.withUpdatedBody(id = 1, body = posts[0].body))
    }

    @Test
    fun `withoutPost removes only the matching post and preserves order`() {
        val result = posts.withoutPost(id = 2)

        assertEquals(listOf(1, 3), result.map { it.id })
    }

    @Test
    fun `withoutPost keeps the identity of the surviving posts`() {
        val result = posts.withoutPost(id = 2)

        assertSame(posts[0], result[0])
        assertSame(posts[2], result[1])
    }

    @Test
    fun `withoutPost returns the same list instance when the id is missing`() {
        assertSame(posts, posts.withoutPost(id = 99))
    }

    @Test
    fun `withoutPost can empty the list`() {
        assertEquals(emptyList<Post>(), listOf(post(1)).withoutPost(id = 1))
    }

    @Test
    fun `findById returns the matching post or null`() {
        assertEquals(posts[1], posts.findById(2))
        assertNull(posts.findById(99))
    }

    @Test
    fun `operations do not mutate the receiver`() {
        val original = posts.toList()

        posts.withUpdatedTitle(id = 1, title = "changed")
        posts.withUpdatedBody(id = 1, body = "changed")
        posts.withoutPost(id = 1)

        assertEquals(original, posts)
    }

    private fun post(id: Int) = Post(
        id = id,
        userId = 1,
        title = "Title $id",
        body = "Body $id",
    )
}
