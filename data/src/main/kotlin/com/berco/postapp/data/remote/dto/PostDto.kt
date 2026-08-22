package com.berco.postapp.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PostDto(
    val id: Int,
    val userId: Int,
    val title: String?,
    val body: String?,
)
