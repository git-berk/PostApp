package com.berco.postapp.data.mapper

import com.berco.postapp.data.remote.dto.PostDto
import com.berco.postapp.domain.model.Post

fun List<PostDto>.toDomain(): List<Post> = map { dto -> dto.toDomain() }

private fun PostDto.toDomain() = Post(
    id = id,
    userId = userId,
    title = title.orEmpty(),
    body = body.orEmpty()
)
