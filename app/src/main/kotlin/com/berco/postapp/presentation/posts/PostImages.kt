package com.berco.postapp.presentation.posts

import com.berco.postapp.domain.model.Post

private const val THUMB = 300
private const val HERO_WIDTH = 490
private const val HERO_HEIGHT = 320

fun Post.thumbnailUrl(): String = picsumUrl(THUMB, THUMB, imageIndex)

fun Post.heroUrl(): String = picsumUrl(HERO_WIDTH, HERO_HEIGHT, imageIndex)

private fun picsumUrl(width: Int, height: Int, index: Int) =
    "https://picsum.photos/seed/${index + 1}/$width/$height?grayscale"
