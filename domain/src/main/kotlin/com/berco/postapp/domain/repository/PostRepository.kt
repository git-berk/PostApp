package com.berco.postapp.domain.repository

import com.berco.postapp.domain.model.Post
import com.berco.postapp.domain.result.AppResult

interface PostRepository {
    suspend fun getPosts(): AppResult<List<Post>>
}
