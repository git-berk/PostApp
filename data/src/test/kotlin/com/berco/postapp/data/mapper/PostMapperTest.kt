package com.berco.postapp.data.mapper

import com.berco.postapp.data.remote.dto.PostDto
import org.junit.Assert.assertEquals
import org.junit.Test

class PostMapperTest {

    @Test
    fun `maps every field across to the domain model`() {
        val result = listOf(dto(id = 7, userId = 3, title = "t", body = "b")).toDomain()

        val post = result.single()
        assertEquals(7, post.id)
        assertEquals(3, post.userId)
        assertEquals("t", post.title)
        assertEquals("b", post.body)
    }

    @Test
    fun `assigns imageIndex from the position in the fetched list`() {
        val result = listOf(dto(id = 10), dto(id = 20), dto(id = 30)).toDomain()

        assertEquals(listOf(0, 1, 2), result.map { it.imageIndex })
    }

    // imageIndex is positional, not derived from the id, so a non-contiguous or
    // unsorted response still yields 0..n-1.
    @Test
    fun `imageIndex is independent of the post id`() {
        val result = listOf(dto(id = 99), dto(id = 4)).toDomain()

        assertEquals(listOf(0, 1), result.map { it.imageIndex })
    }

    @Test
    fun `collapses embedded newlines and runs of whitespace`() {
        val result = listOf(
            dto(title = "two\nlines", body = "spaced   out\n\ttext"),
        ).toDomain()

        val post = result.single()
        assertEquals("two lines", post.title)
        assertEquals("spaced out text", post.body)
    }

    @Test
    fun `trims leading and trailing whitespace`() {
        val post = listOf(dto(title = "  padded  ", body = "\n body \n")).toDomain().single()

        assertEquals("padded", post.title)
        assertEquals("body", post.body)
    }

    @Test
    fun `substitutes empty strings for null title and body`() {
        val post = listOf(dto(title = null, body = null)).toDomain().single()

        assertEquals("", post.title)
        assertEquals("", post.body)
    }

    @Test
    fun `maps an empty response to an empty list`() {
        assertEquals(emptyList<PostDto>(), emptyList<PostDto>().toDomain())
    }

    private fun dto(
        id: Int = 1,
        userId: Int = 1,
        title: String? = "Title",
        body: String? = "Body",
    ) = PostDto(id = id, userId = userId, title = title, body = body)
}
