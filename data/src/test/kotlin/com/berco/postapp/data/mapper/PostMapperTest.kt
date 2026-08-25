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
