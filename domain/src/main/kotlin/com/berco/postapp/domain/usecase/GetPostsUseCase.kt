package com.berco.postapp.domain.usecase

import com.berco.postapp.domain.model.Post
import com.berco.postapp.domain.repository.PostRepository
import com.berco.postapp.domain.result.AppResult
import javax.inject.Inject

class GetPostsUseCase @Inject constructor(
    private val repository: PostRepository,
) {
    suspend operator fun invoke(): AppResult<List<Post>> = repository.getPosts()
}
