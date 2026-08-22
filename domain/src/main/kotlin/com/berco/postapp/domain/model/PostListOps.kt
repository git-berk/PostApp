package com.berco.postapp.domain.model

fun List<Post>.withUpdatedTitle(id: Int, title: String): List<Post> =
    replaceById(id) { it.copy(title = title) }

fun List<Post>.withUpdatedBody(id: Int, body: String): List<Post> =
    replaceById(id) { it.copy(body = body) }

fun List<Post>.withoutPost(id: Int): List<Post> {
    val index = indexOfFirst { it.id == id }
    if (index == -1) return this

    return toMutableList().apply { removeAt(index) }
}

fun List<Post>.findById(id: Int): Post? =
    firstOrNull { it.id == id }

private inline fun List<Post>.replaceById(id: Int, transform: (Post) -> Post): List<Post> {
    val index = indexOfFirst { it.id == id }
    if (index == -1) return this

    val updated = transform(this[index])
    if (updated == this[index]) return this

    return toMutableList().apply { set(index, updated) }
}
