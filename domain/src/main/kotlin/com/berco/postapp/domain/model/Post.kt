package com.berco.postapp.domain.model

data class Post(
    val id: Int,
    val userId: Int,
    val title: String,
    val body: String,
    val imageIndex: Int,
)
