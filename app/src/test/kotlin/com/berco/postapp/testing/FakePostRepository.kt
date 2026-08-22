package com.berco.postapp.testing

import com.berco.postapp.domain.model.Post
import com.berco.postapp.domain.repository.PostRepository
import com.berco.postapp.domain.result.AppError
import com.berco.postapp.domain.result.AppResult

class FakePostRepository(
    var result: AppResult<List<Post>> = AppResult.Success(emptyList()),
) : PostRepository {

    var callCount = 0
        private set

    override suspend fun getPosts(): AppResult<List<Post>> {
        callCount++
        return result
    }

    fun returns(posts: List<Post>) {
        result = AppResult.Success(posts)
    }

    fun fails(error: AppError = AppError.Network) {
        result = AppResult.Failure(error)
    }
}

fun post(
    id: Int,
    title: String = "Title $id",
    body: String = "Body $id",
    userId: Int = 1,
    imageIndex: Int = id - 1,
) = Post(id = id, userId = userId, title = title, body = body, imageIndex = imageIndex)

fun posts(count: Int): List<Post> = (1..count).map { post(it) }
