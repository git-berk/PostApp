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
            post(id = 1).thumbnailUrl(),
        )
    }

    @Test
    fun `hero requests 490x320 from the same seed as the thumbnail`() {
        assertEquals("https://picsum.photos/seed/5/490/320?grayscale", heroUrl(5))
        assertEquals("https://picsum.photos/seed/5/300/300?grayscale", post(id = 5).thumbnailUrl())
    }

    // picsum hashes the seeds "0" and "1" to the same photograph, which would put an
    // identical image on the first two rows. Post ids are 1-based, so seeding on them
    // directly keeps those rows distinct.
    @Test
    fun `seeds are the post ids so the first two rows differ`() {
        assertEquals("https://picsum.photos/seed/1/300/300?grayscale", post(id = 1).thumbnailUrl())
        assertEquals("https://picsum.photos/seed/2/300/300?grayscale", post(id = 2).thumbnailUrl())
    }

    // An edit must not change the row's image.
    @Test
    fun `the url is keyed on the id and ignores the other fields`() {
        assertEquals(
            post(id = 3, title = "before", body = "before").thumbnailUrl(),
            post(id = 3, title = "after", body = "after").thumbnailUrl(),
        )
    }
}
