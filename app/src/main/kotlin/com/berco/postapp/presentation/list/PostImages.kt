package com.berco.postapp.presentation.list

import com.berco.postapp.domain.model.Post

private const val THUMB = 300
private const val HERO_WIDTH = 490
private const val HERO_HEIGHT = 320

fun Post.thumbnailUrl(): String = picsumUrl(THUMB, THUMB, id)
fun heroUrl(postId: Int): String = picsumUrl(HERO_WIDTH, HERO_HEIGHT, postId)
private fun picsumUrl(width: Int, height: Int, seed: Int) =
    "https://picsum.photos/seed/$seed/$width/$height?grayscale"
