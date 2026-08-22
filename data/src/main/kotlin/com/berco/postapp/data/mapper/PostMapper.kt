package com.berco.postapp.data.mapper

import com.berco.postapp.data.remote.dto.PostDto
import com.berco.postapp.domain.model.Post

fun List<PostDto>.toDomain(): List<Post> = mapIndexed { index, dto -> dto.toDomain(index) }

private fun PostDto.toDomain(index: Int) = Post(
    id = id,
    userId = userId,
    title = title.orEmpty().normalizeWhitespace(),
    body = body.orEmpty().normalizeWhitespace(),
    imageIndex = index,
)
private fun String.normalizeWhitespace() = replace(Regex("\\s+"), " ").trim()
