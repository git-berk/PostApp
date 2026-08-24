package com.berco.postapp.presentation.posts

import com.berco.postapp.presentation.list.heroUrl
import com.berco.postapp.presentation.list.thumbnailUrl
import com.berco.postapp.testing.post
import org.junit.Assert.assertEquals
import org.junit.Test

class PostImagesTest {

    @Test
    fun `thumbnail requests the specified 300x300 square`() {
        assertEquals(
            "https://picsum.photos/seed/1/300/300?grayscale",
            post(id = 1, imageIndex = 0).thumbnailUrl(),
        )
    }

    @Test
    fun `hero requests 490x320 from the same seed as the thumbnail`() {
        val post = post(id = 1, imageIndex = 4)

        assertEquals("https://picsum.photos/seed/5/490/320?grayscale", post.heroUrl())
        assertEquals("https://picsum.photos/seed/5/300/300?grayscale", post.thumbnailUrl())
    }

    // picsum hashes the seeds "0" and "1" to the same photograph, which put an
    // identical image on the first two rows.
    @Test
    fun `seeds start at one so the first two rows differ`() {
        val first = post(id = 1, imageIndex = 0).thumbnailUrl()
        val second = post(id = 2, imageIndex = 1).thumbnailUrl()

        assertEquals("https://picsum.photos/seed/1/300/300?grayscale", first)
        assertEquals("https://picsum.photos/seed/2/300/300?grayscale", second)
    }

    @Test
    fun `the url is keyed on imageIndex and not on the post id`() {
        assertEquals(
            post(id = 99, imageIndex = 3).thumbnailUrl(),
            post(id = 7, imageIndex = 3).thumbnailUrl(),
        )
    }
}
